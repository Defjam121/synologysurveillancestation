/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.synologysurveillancestation.internal;

import static org.openhab.binding.synologysurveillancestation.SynologySurveillanceStationBindingConstants.CHANNEL_IMAGE;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.synologysurveillancestation.handler.SynologySurveillanceStationHandler;
import org.openhab.binding.synologysurveillancestation.internal.webapi.WebApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pav
 *
 */
@NonNullByDefault
public class SynoApiThreadSnapshot extends SynoApiThread {
    private final Logger logger = LoggerFactory.getLogger(SynoApiThreadSnapshot.class);

    public SynoApiThreadSnapshot(SynologySurveillanceStationHandler handler, int refreshRate) {
        super(handler, refreshRate);
    }

    @Override
    public boolean refresh() {
        if (getHandler().isLinked(CHANNEL_IMAGE)) {
            Channel channel = getHandler().getThing().getChannel(CHANNEL_IMAGE);
            Thing thing = getHandler().getThing();

            logger.trace("Will update: {}::{}::{}", thing.getUID().getId(), channel.getChannelTypeUID().getId(),
                    thing.getLabel());

            try {
                byte[] snapshot = getApiHandler().getSnapshot(getHandler().getCameraId()).toByteArray();
                getHandler().updateState(channel.getUID(), new RawType(snapshot, "image/jpeg"));
                return true;
            } catch (URISyntaxException | IOException | WebApiException | NullPointerException e) {
                logger.error("could not get snapshot: {}", thing, e);
                return false;
            }
        }
        return true;
    }

}
