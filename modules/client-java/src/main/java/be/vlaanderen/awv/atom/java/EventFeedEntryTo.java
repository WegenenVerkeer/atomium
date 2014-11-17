package be.vlaanderen.awv.atom.java;

import lombok.Data;

import java.util.List;

@Data
public class EventFeedEntryTo {

    private String type; // vb. "DOSSIER_LINK",
    private String uri;
    private List<FeedLinkTo> links; // vb. types "data", "view", "edit"
    private String dossier;

}