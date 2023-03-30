package com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel;


import android.os.Parcel;
import android.os.Parcelable;

/***
 * Represents geometry of the <code>DetailsResponseItem</code> object in terms of latitude, longitude and viewport (if available)
 */
public class Geometry implements Parcelable {
    private String type;
    private Double[] coordinates;
    private Location location;
    /***
     * the Constructor
     */
    protected Geometry(){
    }

    /***
     * Constructor creating object from the parcel.
     * @param in
     */
    protected Geometry(Parcel in) {
        location = in.readParcelable(Location.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(location, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Geometry> CREATOR = new Creator<Geometry>() {
        @Override
        public Geometry createFromParcel(Parcel in) {
            return new Geometry(in);
        }

        @Override
        public Geometry[] newArray(int size) {
            return new Geometry[size];
        }
    };

    /***
     * Returns location object with latitude and longitude.
     * @return Location object
     */
    public Location getLocation() {
        return location;
    }

    /***
     * Sets location
     * @param location
     */
    protected void setLocation(Location location) {
        this.location = location;
    }

    /***
     * Returns the type of the geometry. In this case it will always be Point?
     * @return
     */
    public String getType() { return type;}

    /***
     * Sets geometry type
     * @param type
     */
    public void setType(String type) { this.type = type;}

    /***
     * Gets the coordinates of the geometry
     * @return
     */
    public Double[] getCoordinates() { return coordinates; }

    /***
     * Sets the coordinates of the geometry
     * @param coordinates
     */
    public void setCoordinates(Double[] coordinates) { this.coordinates = coordinates; }

}
