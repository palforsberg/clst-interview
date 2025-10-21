package io.clearstreet.swdn.position;

import io.clearstreet.swdn.model.*;
import io.clearstreet.swdn.refdata.ReferenceDataRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.clearstreet.swdn.Fixtures.*;

@ExtendWith(MockitoExtension.class)
class PositionManagerTest {

  public static final double DELTA = 0.001;
  @Mock(strictness = Strictness.LENIENT)
  private ReferenceDataRepository referenceDataRepository;

  @Test
  void enterSingleBuyTrade() {
    // Given
    PositionManager positionManager = getPositionManager();

    // When
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.NEW,
            50.0)));

    // Then
    Assertions.assertEquals(1,
        positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());
    Position position = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName())
        .get(0);
    Assertions.assertEquals(100.0, position.quantity(), DELTA);
    Assertions.assertEquals(IBM_STOCK.instrumentName(), position.instrumentName());
    Assertions.assertEquals(5000.0, position.initialValue(), DELTA);
  }

  @Test
  void enterMultipleBuyTrade() {
    PositionManager positionManager = getPositionManager();

    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
                    TradeSide.BUY, TradeType.NEW,
                    50.0)));
    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade2", JP_MORGAN_ACCOUNT_1.accountName(), TSLA_STOCK.instrumentName(), 100.0,
                    TradeSide.BUY, TradeType.NEW,
                    50.0)));
    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade3", JP_MORGAN_ACCOUNT_2.accountName(), TSLA_STOCK.instrumentName(), 100.0,
                    TradeSide.BUY, TradeType.NEW,
                    50.0)));

    // Then
    Assertions.assertEquals(2,
            positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());
    Position position1 = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName())
            .get(0);
    System.out.println(position1.toString());
    Assertions.assertEquals(100.0, position1.quantity(), DELTA);
    Assertions.assertEquals(TSLA_STOCK.instrumentName(), position1.instrumentName());
    Assertions.assertEquals(5000.0, position1.initialValue(), DELTA);
    Position position2 = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_2.accountName())
            .get(0);
    Assertions.assertEquals(100.0, position2.quantity(), DELTA);
    Assertions.assertEquals(TSLA_STOCK.instrumentName(), position2.instrumentName());
    Assertions.assertEquals(5000.0, position2.initialValue(), DELTA);
  }

  @Test
  void enterSingleSellTrade() {
    // Given
    PositionManager positionManager = getPositionManager();

    // When
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.SELL, TradeType.NEW,
            50.0)));

    // Then
    Assertions.assertEquals(1,
        positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());
    Position position = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName())
        .get(0);
    Assertions.assertEquals(-100.0, position.quantity(), DELTA);
    Assertions.assertEquals(IBM_STOCK.instrumentName(), position.instrumentName());
    Assertions.assertEquals(-5000.0, position.initialValue(), DELTA);
  }

  @Test
  void enterNetZeroTrades() {
    // Given
    PositionManager positionManager = getPositionManager();

    // When
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.NEW,
            50.0)));
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade2", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.SELL, TradeType.NEW,
            49.0)));

    // Then
    Assertions.assertEquals(1,
        positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());
    Position position = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName())
        .get(0);
    Assertions.assertEquals(0.0, position.quantity(), DELTA);
    Assertions.assertEquals(IBM_STOCK.instrumentName(), position.instrumentName());
    Assertions.assertEquals(100.0, position.initialValue(), DELTA);
  }

  @Test
  void cancelTrade() {
    // Given
    PositionManager positionManager = getPositionManager();

    // When
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.NEW,
            50.0)));
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.CANCEL,
            50.0)));

    // Then
    Assertions.assertEquals(0,
        positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());
  }

  @Test
  void amendTradePrice() {
    // Given
    PositionManager positionManager = getPositionManager();

    // When
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.NEW,
            50.0)));
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.REPLACE,
            51.0)));

    // Then
    Assertions.assertEquals(1,
        positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());

    Position position = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName())
        .get(0);
    Assertions.assertEquals(5100.0, position.initialValue(), DELTA);
    Assertions.assertEquals(100.0, position.quantity(), DELTA);
  }

  @Test
  void amendNonExistingTrade() {
    // Given
    PositionManager positionManager = getPositionManager();

    // When
    Assertions.assertTrue(positionManager.enterTrade(
        new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.NEW,
            50.0)));
    Assertions.assertFalse(positionManager.enterTrade(
        new Trade("trade2", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
            TradeSide.BUY, TradeType.REPLACE,
            51.0)));

    // Then
    Assertions.assertEquals(1,
        positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());

    Position position = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName())
        .get(0);
    Assertions.assertEquals(5000.0, position.initialValue(), DELTA);
    Assertions.assertEquals(100.0, position.quantity(), DELTA);
  }

  @Test
  @DisplayName("Ensure Position Manager maintains state")
  void testPositionManagerMaintainsPositionAfterTradeActivity() {
    // Given
    PositionManager positionManager = getPositionManager();
    referenceDataRepository.enterInstrument(IBM_STOCK);
    referenceDataRepository.enterMember(JP_MORGAN);
    referenceDataRepository.enterAccount(JP_MORGAN_ACCOUNT_1);
    referenceDataRepository.enterAccount(JP_MORGAN_ACCOUNT_2);

    // When
    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade1", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
                    TradeSide.BUY, TradeType.NEW,
                    50.0)));
    //fake replace
    Assertions.assertFalse(positionManager.enterTrade(
            new Trade("trade2", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
                    TradeSide.BUY, TradeType.REPLACE,
                    51.0)));
    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade2", JP_MORGAN_ACCOUNT_1.accountName(), IBM_STOCK.instrumentName(), 100.0,
                    TradeSide.SELL, TradeType.NEW,
                    51.0)));
    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade3", JP_MORGAN_ACCOUNT_1.accountName(), TSLA_STOCK.instrumentName(), 100.0,
                    TradeSide.BUY, TradeType.NEW,
                    49.0)));
    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade4", JP_MORGAN_ACCOUNT_2.accountName(), TSLA_STOCK.instrumentName(), 100.0,
                    TradeSide.BUY, TradeType.NEW,
                    49.0)));
    Assertions.assertTrue(positionManager.enterTrade(
            new Trade("trade5", JP_MORGAN_ACCOUNT_2.accountName(), IBM_STOCK.instrumentName(), 100.0,
                    TradeSide.SELL, TradeType.NEW,
                    53.0)));

    List<Account> accounts = new ArrayList<>();
    accounts.add(JP_MORGAN_ACCOUNT_1);
    accounts.add(JP_MORGAN_ACCOUNT_2);
    Mockito.when(referenceDataRepository.getAccountsForMember(JP_MORGAN.memberName())).thenReturn(accounts);

//     Then
    Assertions.assertEquals(2,
            positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName()).size());
    Assertions.assertEquals(2,
            positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_2.accountName()).size());

    Position position = positionManager.getPositionsForAccount(JP_MORGAN_ACCOUNT_1.accountName())
            .get(0);
    Assertions.assertEquals(TSLA_STOCK.instrumentName(), position.instrumentName());
    Assertions.assertEquals(4900.0, position.initialValue(), DELTA);
    Assertions.assertEquals(100.0, position.quantity(), DELTA);

    Assertions.assertEquals(2,
            positionManager.getPositionsForMember(JP_MORGAN.memberName()).size());
  }

  private PositionManager getPositionManager() {
    PositionManager positionManager = new PositionManager(referenceDataRepository);
    Mockito.when(referenceDataRepository.getInstrument(IBM_STOCK.instrumentName()))
        .thenReturn(Optional.of(IBM_STOCK));
    Mockito.when(referenceDataRepository.getMember(JP_MORGAN.memberName()))
        .thenReturn(Optional.of(JP_MORGAN));
    Mockito.when(referenceDataRepository.getAccount(JP_MORGAN_ACCOUNT_1.accountName()))
        .thenReturn(Optional.of(JP_MORGAN_ACCOUNT_1));
    return positionManager;
  }
}