/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.deobfuscation;

import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;

/**
 * Progressive.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/15 umjammer initial version <br>
 */
public interface Progressive {

    void setProgress(int progress);

    class SwingProgressive implements Progressive {
        public ProgressMonitor progress;

        SwingProgressive(JProgressBar Progress) {
            this.progress = new ProgressMonitor(Progress, null, null, 0, 100);
        }

        @Override
        public void setProgress(int progress) {
            this.progress.setProgress(progress);
        }
    }
}

/* */
