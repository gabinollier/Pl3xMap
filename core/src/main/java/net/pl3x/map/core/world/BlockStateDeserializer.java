/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.pl3x.map.core.world;

import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.pl3x.map.core.Pl3xMap;
import org.jspecify.annotations.Nullable;

public class BlockStateDeserializer implements TypeDeserializer<BlockState> {

    @Override
    public BlockState read(NBTReader reader) throws IOException {
        // 1.26.3+ serializes block states using BlockState.CODEC:
        //   - default state -> plain string ("minecraft:stone")
        //   - custom state  -> compound ({id: "...", properties: {...}})
        // String entries inside a heterogeneous NBT list are wrapped as {"": "..."}
        if (reader.peek() == TagType.STRING) {
            return getBlockState(reader.nextString(), null);
        }

        reader.beginCompound();

        String id = null;
        Map<String, String> properties = null;

        while (reader.hasNext()) {
            switch (reader.name()) {
                case "Name", "id" -> id = reader.nextString();
                case "Properties", "properties" -> {
                    properties = new LinkedHashMap<>();
                    reader.beginCompound();
                    while (reader.hasNext())
                        properties.put(reader.name(), reader.nextString());
                    reader.endCompound();
                }
                case "" -> {
                    // unwrap {"": "minecraft:stone"} written for non-compound
                    // entries of a heterogeneous NBT list
                    if (reader.peek() == TagType.STRING) {
                        id = reader.nextString();
                    } else {
                        reader.skip();
                    }
                }
                default -> reader.skip();
            }
        }

        reader.endCompound();

        return getBlockState(id, properties);
    }

    private BlockState getBlockState(@Nullable String id, @Nullable Map<String, String> properties) {
        if (id == null) return Blocks.AIR.getDefaultState();

        Block block = Pl3xMap.api().getBlockRegistry().getOrDefault(id, Blocks.AIR);
        return properties == null ? new BlockState(block) : new BlockState(block, properties);
    }

}
