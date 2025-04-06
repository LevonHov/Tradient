package com.example.tradient.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Ticker implements Parcelable {
    private double bidPrice;
    private double askPrice;
    private double lastPrice;
    private double volume;
    private Date timestamp;
    private double openPrice;

    // Order book depth information
    private double bidAmount = 0.0;
    private double askAmount = 0.0;
    private double highPrice = 0.0;
    private double lowPrice = 0.0;

    public Ticker(double bidPrice, double askPrice, double lastPrice, double volume, Date timestamp) {
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.lastPrice = lastPrice;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public Ticker(){}

    protected Ticker(Parcel in) {
        bidPrice = in.readDouble();
        askPrice = in.readDouble();
        lastPrice = in.readDouble();
        volume = in.readDouble();
        long tmpTimestamp = in.readLong();
        timestamp = tmpTimestamp == -1 ? null : new Date(tmpTimestamp);
        openPrice = in.readDouble();
        bidAmount = in.readDouble();
        askAmount = in.readDouble();
        highPrice = in.readDouble();
        lowPrice = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(bidPrice);
        dest.writeDouble(askPrice);
        dest.writeDouble(lastPrice);
        dest.writeDouble(volume);
        dest.writeLong(timestamp != null ? timestamp.getTime() : -1);
        dest.writeDouble(openPrice);
        dest.writeDouble(bidAmount);
        dest.writeDouble(askAmount);
        dest.writeDouble(highPrice);
        dest.writeDouble(lowPrice);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Ticker> CREATOR = new Creator<Ticker>() {
        @Override
        public Ticker createFromParcel(Parcel in) {
            return new Ticker(in);
        }

        @Override
        public Ticker[] newArray(int size) {
            return new Ticker[size];
        }
    };

    public void setBidPrice(double bidPrice) {
        this.bidPrice = bidPrice;
    }

    public void setAskPrice(double askPrice) {
        this.askPrice = askPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public double getAskPrice() {
        return askPrice;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getVolume() {
        return volume;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the bid order amount (market depth on the buy side)
     * @return Bid amount in base currency units
     */
    public double getBidAmount() {
        return bidAmount;
    }
    
    /**
     * Sets the bid order amount
     * @param bidAmount Bid amount in base currency units
     */
    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
    
    /**
     * Gets the ask order amount (market depth on the sell side)
     * @return Ask amount in base currency units
     */
    public double getAskAmount() {
        return askAmount;
    }
    
    /**
     * Sets the ask order amount
     * @param askAmount Ask amount in base currency units
     */
    public void setAskAmount(double askAmount) {
        this.askAmount = askAmount;
    }
    
    /**
     * Gets the 24h high price
     * @return Highest price within the last 24 hours
     */
    public double getHighPrice() {
        return highPrice;
    }
    
    /**
     * Sets the 24h high price
     * @param highPrice Highest price within the last 24 hours
     */
    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }
    
    /**
     * Gets the 24h low price
     * @return Lowest price within the last 24 hours
     */
    public double getLowPrice() {
        return lowPrice;
    }
    
    /**
     * Sets the 24h low price
     * @param lowPrice Lowest price within the last 24 hours
     */
    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }

    /**
     * Gets the opening price from the current trading period.
     * 
     * @return The opening price.
     */
    public double getOpenPrice() {
        return openPrice;
    }
    
    /**
     * Sets the opening price for the current trading period.
     * 
     * @param openPrice The opening price.
     */
    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }
}
