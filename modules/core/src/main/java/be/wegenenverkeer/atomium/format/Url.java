package be.wegenenverkeer.atomium.format;

/**
 * Created by Karel Maesen, Geovise BVBA on 30/09/16.
 */
public class Url {

    private final String path;

    public Url(String path) {
        this.path = path;
    }


    public String getPath() {
        return this.path;
    }

    public Url add(Url otherUrl){
        return this.add(otherUrl.getPath());
    }

    public Url add(String additional) {
        boolean startsWith = additional.startsWith("/");
        boolean endsWith = this.path.endsWith("/");

        if (startsWith && endsWith) {
            return new Url(this.path + additional.substring(1));
        } else if (startsWith || endsWith) {
            return new Url(this.path+additional);
        } else {
            return new Url(this.path + "/" + additional);
        }
    }

    public Url add(Integer additionalPath) {
        return add(additionalPath.toString());
    }

    public Url add(Long additionalPath) {
        return add(additionalPath.toString());
    }

}
