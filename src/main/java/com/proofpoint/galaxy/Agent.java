/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.galaxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import java.io.File;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Agent
{
    private final AtomicInteger nextId = new AtomicInteger();
    private final UUID agentId = UUID.randomUUID(); // todo make persistent
    private final ConcurrentMap<String, Slot> slots;
    private final AgentConfig config;
    private final File slotDir;
    private final DeploymentManagerFactory deploymentManager;
    private final LifecycleManager lifecycleManager;

    @Inject
    public Agent(AgentConfig config, DeploymentManagerFactory deploymentManagerFactory, LifecycleManager lifecycleManager)
    {
        Preconditions.checkNotNull(config, "config is null");
        Preconditions.checkNotNull(deploymentManagerFactory, "deploymentManagerFactory is null");
        Preconditions.checkNotNull(lifecycleManager, "lifecycleManager is null");

        this.config = config;
        this.slotDir = new File(config.getSlotsDir());

        slotDir.mkdirs();
        if (!slotDir.isDirectory()) {
            throw new IllegalArgumentException("slotDir is not a directory");
        }

        this.deploymentManager = deploymentManagerFactory;
        this.lifecycleManager = lifecycleManager;

        slots = new ConcurrentHashMap<String, Slot>();
    }

    public UUID getAgentId()
    {
        return agentId;
    }

    public Slot getSlot(String name)
    {
        Preconditions.checkNotNull(name, "name must not be null");

        Slot slot = slots.get(name);
        return slot;
    }

    public Slot addNewSlot()
    {
        String slotName = "slot" + nextId.incrementAndGet();
        Slot slot = new Slot(slotName, config, deploymentManager.createDeploymentManager(new File(slotDir, slotName)), lifecycleManager);
        slots.put(slotName, slot);
        return slot;
    }

    public boolean deleteSlot(String name)
    {
        Preconditions.checkNotNull(name, "name must not be null");

        Slot slot = slots.remove(name);
        if (slot == null) {
            return false;
        }

        slot.clear();
        return true;
    }

    public Collection<Slot> getAllSlots()
    {
        return ImmutableList.copyOf(slots.values());
    }
}
