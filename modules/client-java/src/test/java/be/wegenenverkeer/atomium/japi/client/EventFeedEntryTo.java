package be.wegenenverkeer.atomium.japi.client;


import be.wegenenverkeer.atomium.japi.format.Link;

import java.util.List;

public class EventFeedEntryTo {

    private String type; // vb. "DOSSIER_LINK",
    private String uri;
    private List<Link> links; // vb. types "data", "view", "edit"
    private String dossier;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public String getDossier() {
        return dossier;
    }

    public void setDossier(String dossier) {
        this.dossier = dossier;
    }
}