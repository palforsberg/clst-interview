package io.clearstreet.swdn.position;

import io.clearstreet.swdn.api.PositionApi;
import io.clearstreet.swdn.api.TradeApi;
import io.clearstreet.swdn.model.Position;
import io.clearstreet.swdn.model.Trade;
import io.clearstreet.swdn.refdata.ReferenceDataRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PositionManager implements TradeApi, PositionApi {

  private final ReferenceDataRepository referenceDataManager;
  private final List<Trade> trades = new ArrayList<>();

  public PositionManager(ReferenceDataRepository referenceDataManager) {
    this.referenceDataManager = referenceDataManager;
  }

  @Override
  public boolean enterTrade(Trade trade) {
    trades.add(trade);
    return true;
  }

  @Override
  public List<Position> getPositionsForMember(String memberName) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public List<Position> getPositionsForAccount(String accountName) {
    Map<PositionKey, Position> positions = new HashMap<>();
    for (Trade trade : trades) {
      if (trade.accountName().equals(accountName)) {
        PositionKey key = new PositionKey(trade.accountName(), trade.instrumentName());
        Position position = positions.get(key);
        if (position == null) {
          position = new Position(trade.accountName(), trade.instrumentName(), 0, 0);
        }
        positions.put(key, new Position(trade.accountName(), trade.instrumentName(),
            position.quantity() + trade.quantity(),
            position.initialValue() + trade.quantity() * trade.price()));
      }
    }
    return new ArrayList<>(positions.values());
  }

  private record PositionKey(String accountName, String instrumentName) {

  }
}
