package com.cgi.eoss.fstep.io.download;

import java.net.URI;
import java.util.Comparator;

public final class DescendingPriorityUriComparator implements Comparator<Downloader> {
    private final URI uri;

    public DescendingPriorityUriComparator(URI uri) {
        this.uri = uri;
    }

    @Override
    public int compare(Downloader o1, Downloader o2) {
        return Integer.compare(o2.getPriority(uri), o1.getPriority(uri));
    }
}
