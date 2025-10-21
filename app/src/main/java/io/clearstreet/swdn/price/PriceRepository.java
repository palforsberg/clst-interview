package io.clearstreet.swdn.price;

import io.clearstreet.swdn.api.PriceApi;
import io.clearstreet.swdn.model.Price;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PriceRepository implements PriceApi {

  Map<String, Price> prices = new HashMap<>();

  @Override
  public boolean enterPrice(Price price) {
    prices.put(price.instrumentName(), price);
      try {
          saveState();
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      return true;
  }

  public Optional<Double> getPrice(String instrumentName) {
    return Optional
        .ofNullable(prices.get(instrumentName))
        .map(Price::price);
  }

  public void saveState() throws IOException {
    File file = new File("recovery/price_repo.ser");

    File parentDir = file.getParentFile();
    if (!parentDir.exists() && !parentDir.mkdirs()) {
      throw new IOException("Could not create directory: " + parentDir.getAbsolutePath());
    }

    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeObject(prices);
    }
  }

  @SuppressWarnings("unchecked")
  public void loadState() throws IOException, ClassNotFoundException {
    File file = new File("./recovery/price_repo.ser");
    if (!file.exists()) return; // No state to load

    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
      Map<String, Price> loadedPrices = (Map<String, Price>) in.readObject();

      prices.clear();
      prices.putAll(loadedPrices);
    }
  }
}
