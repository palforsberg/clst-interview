package io.clearstreet.swdn.model;

import java.io.Serializable;

/**
 * A price for an instrument.
 */
public record Price(String instrumentName, double price) implements Serializable {

}
