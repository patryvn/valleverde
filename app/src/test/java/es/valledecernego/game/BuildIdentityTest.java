package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Guards against shipping a preview whose in-game version and launcher identity disagree. */
public class BuildIdentityTest {
    @Test public void previewBuildVersionIsExplicitAndCurrent() {
        assertEquals("0.29.8", BuildConfig.VERSION_NAME);
    }
}
