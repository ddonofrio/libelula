/*
 *            This file is part of Libelula Minecraft Edition Project.
 *
 *  Libelula Minecraft Edition is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libelula Minecraft Edition is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libelula Minecraft Edition. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.atlantis;

import java.util.Random;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
class AtlantisGenerator extends ChunkGenerator {

    private final Main plugin;

    public AtlantisGenerator(Main plugin) {
        super();
        this.plugin = plugin;
    }

    @Override
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        byte[][] result = new byte[world.getMaxHeight() / 16][]; //world height / chunk part height (=16, look above)
        int x;
        int y;
        int z;
        for (x = 0; x < 16; x++) {
            for (y = 0; y < 256; y++) {
                for (z = 0; z < 16; z++) {
                    int prob = (int) (Math.random() * 100);
                    Material mat;

                    if (world.getName().equalsIgnoreCase("sea")) {
                        if (y == 0) {
                            setBlock(result, x, y, z, (byte) getWorldSeparatorMaterial(prob).getId());
                        } else if (y <= 60) {
                            setBlock(result, x, y, z, (byte) Material.WATER.getId());
                        }
                        continue;
                    }

                    if (world.getName().equalsIgnoreCase("under")) {
                        //biomeGrid.setBiome(chunkX, chunkZ, Biome.HELL);
                        
                        //setChunck(result, y, generate(world, random, chunkX, chunkZ));
                        continue;
                        //return super.generateBlockSections(world, random, chunkX, chunkZ, biomeGrid);
                        //biomeGrid.setBiome(chunkX, chunkZ, Biome.DEEP_OCEAN);
                        //return result;
                    }

                    if (y > 20) {
                        if (y == 21 && prob == 1) {
                            setBlock(result, x, y, z, (byte) Material.SAND.getId());
                        } else {
                            setBlock(result, x, y, z, (byte) Material.WATER.getId());
                        }
                        continue;
                    }

                    if (chunkX % 10 == 0 && chunkZ % 10 == 0 && x < 5 && z < 5 && y > 0) {
                        if ((x == 0 && z == 0) || (x == 4 && z == 4) || (x == 4 && z == 0) || (x == 0 && z == 4)) {
                            setBlock(result, x, y, z, (byte) Material.GLOWSTONE.getId());
                            continue;
                        }
                        if (prob <= 5) {
                            mat = Material.GOLD_ORE;
                        } else if (prob > 5 && prob <= 10) {
                            mat = Material.EMERALD_ORE;

                        } else if (prob > 10 && prob <= 20) {
                            mat = Material.IRON_ORE;

                        } else if (prob > 20 && prob <= 40) {
                            mat = Material.COAL_ORE;
                        } else if (prob > 40 && prob <= 42) {
                            mat = Material.REDSTONE_ORE;

                        } else if (prob > 42 && prob <= 45) {
                            mat = Material.LAPIS_ORE;
                        } else if (prob == 45) {
                            mat = Material.DIAMOND_ORE;
                        } else if (prob == 46) {
                            mat = Material.QUARTZ_ORE;
                        } else if (prob == 47) {
                            mat = Material.COAL_BLOCK;
                        } else {
                            mat = Material.OBSIDIAN;
                        }
                        setBlock(result, x, y, z, (byte) mat.getId());
                        continue;
                    }

                    switch (y) {
                        case 0:
                            setBlock(result, x, y, z, (byte) Material.BEDROCK.getId());
                            break;
                        case 1:
                            if (prob < 50) {
                                mat = Material.STONE;
                            } else if (prob > 50) {
                                mat = Material.LAVA;
                            } else {
                                mat = Material.AIR;
                            }
                            setBlock(result, x, y, z, (byte) mat.getId());
                            break;
                        case 2:
                            if (prob < 50) {
                                mat = Material.STONE;
                            } else if (prob > 50) {
                                mat = Material.COBBLESTONE;
                            } else {
                                mat = Material.ENDER_STONE;
                            }
                            setBlock(result, x, y, z, (byte) mat.getId());
                            break;
                        case 3:
                        case 4:
                            if (prob < 50) {
                                mat = Material.MOSSY_COBBLESTONE;
                            } else if (prob > 50 && prob < 75) {
                                mat = Material.CLAY;
                            } else if (prob > 75 && prob < 80) {
                                mat = Material.STAINED_CLAY;
                            } else {
                                mat = Material.DIRT;
                            }
                            setBlock(result, x, y, z, (byte) mat.getId());
                            break;
                        case 5:
                            if (prob <= 50) {
                                setBlock(result, x, y, z, (byte) Material.DIRT.getId());
                            } else {
                                setBlock(result, x, y, z, (byte) Material.SAND.getId());
                            }
                            break;
                        case 6:
                            if (biomeGrid.getBiome(x, z).equals(Biome.MUSHROOM_ISLAND)
                                    || biomeGrid.getBiome(x, z).equals(Biome.MUSHROOM_SHORE)) {
                                setBlock(result, x, y, z, (byte) Material.MYCEL.getId());
                            } else {
                                setBlock(result, x, y, z, (byte) Material.GRASS.getId());
                            }
                            break;
                        case 7:
                            mat = null;
                            if (prob < 10) {
                                // Something
                            } else if (prob >= 10 && prob < 15) {
                                // Something
                            } else if (prob >= 15 && prob < 16) {
                                int subRand = (int) (Math.random() * 100);
                                if (subRand == 1) {
                                    mat = Material.SAPLING;
                                } else if (subRand >= 2 && subRand < 5) {
                                    mat = Material.YELLOW_FLOWER;
                                } else if (subRand >= 5 && subRand < 25) {
                                    // Something else
                                    mat = null;
                                } else if (subRand >= 25 && subRand < 26) {
                                    mat = Material.RED_MUSHROOM;
                                } else if (subRand >= 26 && subRand < 27) {
                                    mat = Material.BROWN_MUSHROOM;
                                }
                            }
                            if (mat != null) {
                                setBlock(result, x, y, z, (byte) mat.getId());
                            }
                            break;
                        case 20:
                            setBlock(result, x, y, z, (byte) getWorldSeparatorMaterial(prob).getId());
                            break;
                    }
                }
            }
        }

        return result;
    }

    private Material getWorldSeparatorMaterial(int prob) {
        Material mat;
        if (prob < 50) {
            mat = Material.STAINED_GLASS;
        } else if (prob > 50) {
            mat = Material.GLASS;
        } else {
            mat = Material.GLOWSTONE;
        }
        return mat;
    }

    private void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
        // is this chunk part already initialized?
        if (result[y >> 4] == null) {
            // Initialize the chunk part
            result[y >> 4] = new byte[4096];
        }
        // set the block (look above, how this is done)
        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
    }
    
    private void setChunck(byte[][] result, int y, byte[] chunk) {
        // is this chunk part already initialized?
        if (result[y >> 4] == null) {
            // Initialize the chunk part
            result[y >> 4] = new byte[4096];
        }
        // set the block (look above, how this is done)
        result[y >> 4] = chunk;
    }
    

}
