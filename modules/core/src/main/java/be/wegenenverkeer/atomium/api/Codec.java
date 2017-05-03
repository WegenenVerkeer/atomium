package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 08/12/16.
 */
public interface Codec<VALTYPE, SERIAlIZEDTYPE> {

    public String getMimeType();

    public SERIAlIZEDTYPE encode(VALTYPE value);

    public VALTYPE decode(SERIAlIZEDTYPE encoded);
}
