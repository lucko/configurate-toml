/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.configurate.toml;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import org.junit.jupiter.api.Test;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TOMLConfigurationLoaderTest {
    @Test
    public void testSpec() throws IOException, ObjectMappingException {
        URL url = getClass().getResource("/example.toml");
        TOMLConfigurationLoader loader = TOMLConfigurationLoader.builder().setURL(url).build();
        ConfigurationNode node = loader.load();
        assertEquals("TOML Example", node.getNode("title").getString());
        assertEquals("Tom Preston-Werner", node.getNode("owner", "name").getString());
        assertEquals("GitHub Cofounder & CEO\nLikes tater tots and beer.", node.getNode("owner", "bio").getString());
        assertEquals(Instant.parse("1979-05-27T07:32:00Z"), node.getNode("owner", "dob").getValue(TypeToken.of(Instant.class)));
        assertEquals(ImmutableList.of(8001, 8001, 8002), node.getNode("database", "ports").getList(TypeToken.of(Integer.class)));
        assertTrue(node.getNode("database", "enabled").getBoolean());
        assertEquals("10.0.0.1", node.getNode("servers", "alpha", "ip").getString());
        assertEquals("10.0.0.2", node.getNode("servers", "beta", "ip").getString());
        assertEquals("中国", node.getNode("servers", "beta", "country").getString());
        List<? extends ConfigurationNode> nodes = node.getNode("products").getChildrenList();
        ConfigurationNode node1 = nodes.get(0);
        assertEquals("Hammer", node1.getNode("name").getString());
        assertEquals(738594937, node1.getNode("sku").getInt());
        ConfigurationNode node2 = nodes.get(1);
        assertEquals("Nail", node2.getNode("name").getString());
        assertEquals(284758393, node2.getNode("sku").getInt());
    }
}