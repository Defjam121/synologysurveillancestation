/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.synologysurveillancestation.handler;

import static org.openhab.binding.synologysurveillancestation.SynologySurveillanceStationBindingConstants.*;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.synologysurveillancestation.internal.SynoApiThreadEvent;
import org.openhab.binding.synologysurveillancestation.internal.SynoApiThreadSnapshot;
import org.openhab.binding.synologysurveillancestation.internal.webapi.SynoEvent;
import org.openhab.binding.synologysurveillancestation.internal.webapi.WebApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SynologySurveillanceStationHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils
 */
@NonNullByDefault
public class SynologySurveillanceStationHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SynologySurveillanceStationHandler.class);
    private String cameraId = "";
    private boolean isPtz = false;

    private final SynoApiThreadSnapshot threadSnapshot;
    private final SynoApiThreadEvent threadEvent;

    public SynologySurveillanceStationHandler(Thing thing, boolean isPtz) {
        super(thing);
        this.isPtz = isPtz;
        int refreshRateSnapshot = Integer.parseInt(thing.getConfiguration().get(REFRESH_RATE_SNAPSHOT).toString());
        threadSnapshot = new SynoApiThreadSnapshot(this, refreshRateSnapshot);
        int refreshRateEvents = Integer.parseInt(thing.getConfiguration().get(REFRESH_RATE_EVENTS).toString());
        threadEvent = new SynoApiThreadEvent(this, refreshRateEvents);
    }

    @Override
    public boolean isLinked(String channelId) {
        return super.isLinked(channelId);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            switch (channelUID.getId()) {
                case CHANNEL_IMAGE:
                    if (command.toString().equals("REFRESH")) {
                        threadSnapshot.refresh();
                    }
                    break;
                case CHANNEL_EVENT_MOTION:
                case CHANNEL_EVENT_ALARM:
                case CHANNEL_EVENT_MANUAL:
                    if (command.toString().equals("REFRESH")) {
                        updateState(channelUID, OnOffType.OFF);
                    }
                    break;
                case CHANNEL_RECORD:
                case CHANNEL_ENABLE:
                case CHANNEL_ZOOM:
                case CHANNEL_MOVE:
                    SynologySurveillanceStationBridgeHandler bridge = ((SynologySurveillanceStationBridgeHandler) getBridge()
                            .getHandler());
                    bridge.getSynoWebApiHandler().execute(cameraId, channelUID.getId(), command.toString());
                    break;
            }
        } catch (WebApiException e) {
            logger.error("handle command: {}::{}::{}", getThing().getLabel(), getThing().getUID());
        }

    }

    @Override
    public void dispose() {
        threadSnapshot.stop();
        threadEvent.stop();
    }

    @Override
    public void initialize() {
        if (getBridge() != null) {
            cameraId = getThing().getUID().getId();

            logger.debug("Initializing SynologySurveillanceStationHandler for cameraId '{}'", cameraId);

            if (!isPtz) {
                ThingBuilder thingBuilder = editThing();
                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_ZOOM));
                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_MOVE));
                updateThing(thingBuilder.build());
            }

            if (getBridge().getStatus() == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
                threadSnapshot.start();
                threadEvent.start();

            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Initialize thing: {}::{}", getThing().getLabel(), getThing().getUID());
        }

    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        Configuration configuration = editConfiguration();
        for (Entry<String, Object> configurationParameter : configurationParameters.entrySet()) {
            configuration.put(configurationParameter.getKey(), configurationParameter.getValue());
        }
        updateConfiguration(configuration);

        int refreshRateSnapshot = Integer.parseInt(configurationParameters.get(REFRESH_RATE_SNAPSHOT).toString());
        threadSnapshot.setRefreshRate(refreshRateSnapshot);
        int refreshRateEvents = Integer.parseInt(configurationParameters.get(REFRESH_RATE_EVENTS).toString());
        threadEvent.setRefreshRate(refreshRateEvents);
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        String id = channelUID.getId();
        switch (id) {
            case CHANNEL_EVENT_MOTION:
                threadEvent.getEvents().put(id, new SynoEvent(SynoEvent.EVENT_REASON_MOTION));
                break;
            case CHANNEL_EVENT_ALARM:
                threadEvent.getEvents().put(id, new SynoEvent(SynoEvent.EVENT_REASON_ALARM));
                break;
            case CHANNEL_EVENT_MANUAL:
                threadEvent.getEvents().put(id, new SynoEvent(SynoEvent.EVENT_REASON_MANUAL));
                break;
        }
        handleCommand(channelUID, RefreshType.REFRESH);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        String id = channelUID.getId();
        threadEvent.getEvents().remove(id);
    }

    @Override
    public void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    public @Nullable Bridge getBridge() {
        return super.getBridge();
    }

    @Override
    public void updateState(ChannelUID channelUID, State state) {
        super.updateState(channelUID, state);
    }

    /**
     * @return service scheduler of this Thing
     */
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /**
     * @return the cameraId
     */
    public String getCameraId() {
        return cameraId;
    }

}
