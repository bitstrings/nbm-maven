package org.bitstrings.maven.nbm.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class JnlpUtils
{
    public static final String JNLP_TIMESTAMP_PREFIX = "TS: ";

    public static final String JNLP_TIMESTAMP_DATETIME_FORMAT = "YYYY-MM-DD hh:mm:ss";

    private JnlpUtils() {}

    public static String getCurrentJnlpTimestamp()
    {
        return getJnlpTimestamp(new Date());
    }

    public static String getJnlpTimestamp( Date date )
    {
        final SimpleDateFormat dateFormat = new SimpleDateFormat( JNLP_TIMESTAMP_DATETIME_FORMAT );

        return JNLP_TIMESTAMP_PREFIX + dateFormat.format( date );
    }
}
