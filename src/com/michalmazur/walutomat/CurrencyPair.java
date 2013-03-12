package com.michalmazur.walutomat;

/**
 * CurrencyPair stores currency pair name and rates.
 */
public class CurrencyPair {
	protected String name;
	protected String purchasePrice;
	protected String salePrice;
	protected String rate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		name = name.replaceAll(" ", "");
		this.name = name;
	}

	public String getPurchasePrice() {
		return purchasePrice;
	}

	public void setPurchasePrice(String purchasePrice) {
		this.purchasePrice = purchasePrice;
	}

	public String getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(String salePrice) {
		this.salePrice = salePrice;
	}

	public String getRate() {
		return rate;
	}

	public void setRate(String rate) {
		this.rate = rate;
	}

}
