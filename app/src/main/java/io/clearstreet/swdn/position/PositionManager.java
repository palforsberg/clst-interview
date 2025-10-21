package io.clearstreet.swdn.position;

import io.clearstreet.swdn.api.PositionApi;
import io.clearstreet.swdn.api.TradeApi;
import io.clearstreet.swdn.model.*;
import io.clearstreet.swdn.refdata.ReferenceDataRepository;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PositionManager implements TradeApi, PositionApi {

  private final ReferenceDataRepository referenceDataManager;
  // Much easier to control a map than a list. Especially if TradeId is intended to be unique identifier
  private final Map<String, Trade> trades = new HashMap<>();
  // To avoid iterating over all Trades when getPositionsForMember is called we need new Map
  private final Map<String, List<Trade>> tradesByAccount = new HashMap<>();

  public PositionManager(ReferenceDataRepository referenceDataManager) {
    this.referenceDataManager = referenceDataManager;
  }

  public void saveState() throws IOException {
    File file = new File("recovery/position_manager.ser");

    File parentDir = file.getParentFile();
    if (!parentDir.exists() && !parentDir.mkdirs()) {
      throw new IOException("Could not create directory: " + parentDir.getAbsolutePath());
    }

    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeObject(trades);
      out.writeObject(tradesByAccount);
    }
  }

  @SuppressWarnings("unchecked")
  public void loadState() throws IOException, ClassNotFoundException {
    File file = new File("./recovery/position_manager.ser");
    if (!file.exists()) return; // No state to load

    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
      Map<String, Trade> loadedTrades = (Map<String, Trade>) in.readObject();
      Map<String, List<Trade>> loadedTradesByAccount = (Map<String, List<Trade>>) in.readObject();

      trades.clear();
      trades.putAll(loadedTrades);

      tradesByAccount.clear();
      tradesByAccount.putAll(loadedTradesByAccount);
    }
  }

  public int getTradesSize() {
    return trades.size();
  }

  @Override
  public boolean enterTrade(Trade trade) {
      switch (trade.tradeType()) {
          case NEW -> {
              //update trade cache
              if (trades.putIfAbsent(trade.tradeId(), trade) == null) {
                  //update tradesByAccount
                  tradesByAccount.computeIfAbsent(trade.accountName(), k -> new ArrayList<>()).add(trade);
              }
          }
          case REPLACE -> {
              //update trade cache
              Trade replaced = trades.replace(trade.tradeId(), trade);
              //if updated, update tradesbyAccount
              if (replaced != null) {
                  List<Trade> accountTrades = tradesByAccount.get(trade.accountName());
                  if (accountTrades != null) {
                      // Replace the existing trade in the list
                      accountTrades.stream()
                        .filter(t -> t.tradeId().equals(trade.tradeId()))
                        .findFirst()
                        .ifPresent(existingTrade -> {
                          int index = accountTrades.indexOf(existingTrade);
                          accountTrades.set(index, trade);
                        });
                  }
              } else {
                // If replace on non-existing trade
                return false;
              }
          }
          case CANCEL -> {
              //update trade cache
              Trade cancel = trades.remove(trade.tradeId());
              //if cancelled, update tradesbyAccount
              if (cancel != null) {
                  List<Trade> accountTrades = tradesByAccount.get(cancel.accountName());
                  if (accountTrades != null) {
                      accountTrades.removeIf(t -> t.tradeId().equals(cancel.tradeId()));
                      if (accountTrades.isEmpty()) {
                          tradesByAccount.remove(cancel.accountName());
                      }
                  }
              } else {
                // If cancel on non-existing trade
                return false;
              }
          }
      }
      try {
          //not ideal, ideally save the state of cache to separate caching service
          saveState();
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      return true;
  }

  @Override
  public List<Position> getPositionsForMember(String memberName) {
    Map<PositionKey, Position> positions = new HashMap<>();
    List<Account> accountsByMember = referenceDataManager.getAccountsForMember(memberName);
    Map<String, List<Trade>> accountsWithTrades = getFilteredTradesByAccount(accountsByMember);

    //Nested for loops, however should only be getting positions for trades associated with account associated with member
    for (Map.Entry<String, List<Trade>>  account : accountsWithTrades.entrySet()) {
      List<Trade> accountTrades = account.getValue();
      for (Trade trade : accountTrades) {
        updatePositionsForTrade(trade, positions);
      }
    }
      return new ArrayList<>(positions.values());
  }

  @Override
  public List<Position> getPositionsForAccount(String accountName) {
    Map<PositionKey, Position> positions = new HashMap<>();
    for (Map.Entry<String, Trade> tradeEntry : trades.entrySet()) {
      Trade trade  = tradeEntry.getValue();
      if (trade.accountName().equals(accountName)) {
        updatePositionsForTrade(trade, positions);
      }
    }
    return new ArrayList<>(positions.values());
  }

  private Map<String, List<Trade>> getFilteredTradesByAccount(List<Account> accounts) {
    // Create set for quicker lookup
    Set<String> accountNames = accounts.stream()
            .map(Account::accountName)
            .collect(Collectors.toSet());

    // Filter tradesByAccount to include only those accounts
    return tradesByAccount.entrySet().stream()
            .filter(entry -> accountNames.contains(entry.getKey()))
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
            ));
  }

  private void updatePositionsForTrade(Trade trade, Map<PositionKey, Position> positions) {
    PositionKey key = new PositionKey(trade.instrumentName());
    Position position = positions.get(key);
    if (position == null) {
      position = new Position(trade.accountName(), trade.instrumentName(), 0, 0);
    }
    double signedQuantity = getTradeSignedQuantityGivenSide(trade);
    positions.put(key, new Position(trade.accountName(), trade.instrumentName(),
            position.quantity() + signedQuantity,
            position.initialValue() + signedQuantity * trade.price()));
  }

  private double getTradeSignedQuantityGivenSide(Trade trade) {
    // Quantity must be correctly added/subtracted based on the trade's side
    return trade.side().equals(TradeSide.SELL) ? -trade.quantity() : trade.quantity();
  }

  // Removing accountName from record. At this point, we know which account we're dealing with. We need the instrumentName only
  private record PositionKey(String instrumentName) {

  }
}
