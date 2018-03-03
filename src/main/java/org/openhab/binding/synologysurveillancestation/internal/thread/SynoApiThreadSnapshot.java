/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.synologysurveillancestation.internal.thread;

import static org.openhab.binding.synologysurveillancestation.SynoBindingConstants.CHANNEL_SNAPSHOT;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.synologysurveillancestation.handler.SynoCameraHandler;

/**
 * Thread for getting snapshots
 *
 * @author Pavion
 */
@NonNullByDefault
public class SynoApiThreadSnapshot extends SynoApiThread {
    // private final Logger logger = LoggerFactory.getLogger(SynoApiThreadSnapshot.class);

    public SynoApiThreadSnapshot(SynoCameraHandler handler, int refreshRate) {
        super(SynoApiThread.THREAD_SNAPSHOT, handler, refreshRate);
    }

    @Override
    public boolean isNeeded() {
        return (getAsCameraHandler().isLinked(CHANNEL_SNAPSHOT));
    }

    @Override
    public boolean refresh() throws Exception {
        Channel channel = getAsCameraHandler().getThing().getChannel(CHANNEL_SNAPSHOT);
        Thing thing = getAsCameraHandler().getThing();

        byte[] snapshot = getApiHandler().getSnapshot(getAsCameraHandler().getCameraId(), getRefreshRate());
        if (snapshot.length < 1000) {
            getAsCameraHandler().updateState(channel.getUID(), UnDefType.UNDEF);
            return false;
        } else {
            getAsCameraHandler().updateState(channel.getUID(), new RawType(snapshot, "image/jpeg"));
            return true;
        }
    }

}
