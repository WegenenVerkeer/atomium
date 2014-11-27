package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.JLink;
import lombok.Data;

import java.util.List;

@Data
public class EventFeedEntryTo {

    private String type; // vb. "DOSSIER_LINK",
    private String uri;
    private List<JLink> links; // vb. types "data", "view", "edit"
    private String dossier;

}