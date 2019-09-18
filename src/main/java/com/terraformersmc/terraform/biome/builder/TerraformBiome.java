package com.terraformersmc.terraform.biome.builder;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.CountDecoratorConfig;
import net.minecraft.world.gen.decorator.CountExtraChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.SurfaceConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TerraformBiome extends Biome {
	private int grassColor;
	private int foliageColor;
	private float spawnChance;

	private TerraformBiome(Biome.Settings biomeSettings, ArrayList<SpawnEntry> spawns, float spawnChance) {
		super(biomeSettings);
		for (SpawnEntry entry : spawns) {
			this.addSpawn(entry.type.getCategory(), entry);
		}
		
		this.spawnChance = spawnChance;
	}

	public void setGrassAndFoliageColors(int grassColor, int foliageColor) {
		this.grassColor = grassColor;
		this.foliageColor = foliageColor;
	}

	@Override
	public int getGrassColorAt(BlockPos pos) {
		if (grassColor == -1) {
			return super.getGrassColorAt(pos);
		}

		return grassColor;
	}

	@Override
	public int getFoliageColorAt(BlockPos pos) {
		if (foliageColor == -1) {
			return super.getFoliageColorAt(pos);
		}

		return foliageColor;
	}
	
	@Override
	public float getMaxSpawnLimit() {
		return spawnChance;
	}

	public static TerraformBiome.Builder builder() {
		return new Builder();
	}

	public static final class Builder extends BuilderBiomeSettings {

		private ArrayList<DefaultFeature> defaultFeatures = new ArrayList<>();
		private ArrayList<FeatureEntry> features = new ArrayList<>();
		private Map<StructureFeature<FeatureConfig>, FeatureConfig> structureFeatures = new HashMap<>();
		private Map<Feature<DefaultFeatureConfig>, Integer> treeFeatures = new HashMap<>();
		private Map<Feature<DefaultFeatureConfig>, Integer> rareTreeFeatures = new HashMap<>();
		private Map<BlockState, Integer> plantFeatures = new HashMap<>();
		private Map<BlockState, Integer> doublePlantFeatures = new HashMap<>();
		private ArrayList<SpawnEntry> spawnEntries = new ArrayList<>();
		private int grassColor = -1;
		private int foliageColor = -1;
		private boolean template = false;
		private float spawnChance = 0.1F;
		// NOTE: Make sure to add any additional fields to the Template copy code down below!

		Builder() {
			super();

			parent(null);
		}

		Builder(Builder existing) { // Template copy code
			super(existing);

			this.defaultFeatures.addAll(existing.defaultFeatures);
			this.features.addAll(existing.features);
			this.structureFeatures.putAll(existing.structureFeatures);
			this.treeFeatures.putAll(existing.treeFeatures);
			this.rareTreeFeatures.putAll(existing.rareTreeFeatures);
			this.plantFeatures.putAll(existing.plantFeatures);
			this.doublePlantFeatures.putAll(existing.doublePlantFeatures);
			this.spawnEntries.addAll(existing.spawnEntries);

			this.grassColor = existing.grassColor;
			this.foliageColor = existing.foliageColor;
			this.spawnChance = existing.spawnChance;
		}

		public Biome build() {
			if(template) {
				throw new IllegalStateException("Tried to call build() on a frozen Builder instance!");
			}

			// Add SpawnEntries
			TerraformBiome biome = new TerraformBiome(this, this.spawnEntries, this.spawnChance);

			// Set grass and foliage colors
			biome.setGrassAndFoliageColors(this.grassColor, this.foliageColor);

			// Add structures
			for (Map.Entry<StructureFeature<FeatureConfig>, FeatureConfig> structure : structureFeatures.entrySet()) {
				biome.addStructureFeature(structure.getKey(), structure.getValue());
			}

			// Tree Feature stuff
			if (treeFeatures.size() > 0) {

				// Determine the total tree count

				int totalTreesPerChunk = 0;
				for (Integer count : treeFeatures.values()) {
					totalTreesPerChunk += count;
				}

				// Add each tree

				for (Map.Entry<Feature<DefaultFeatureConfig>, Integer> tree : treeFeatures.entrySet()) {
					Feature<DefaultFeatureConfig> feature = tree.getKey();
					int count = tree.getValue();

					float weight = (float) count / totalTreesPerChunk;

					biome.addFeature(
							GenerationStep.Feature.VEGETAL_DECORATION,
							Biome.configureFeature(
									feature,
									FeatureConfig.DEFAULT,
									Decorator.COUNT_EXTRA_HEIGHTMAP,
									new CountExtraChanceDecoratorConfig(count, 0.1F * weight, 1)
							)
					);
				}
			}

			// Rare tree features

			for (Map.Entry<Feature<DefaultFeatureConfig>, Integer> tree : rareTreeFeatures.entrySet()) {
				Feature<DefaultFeatureConfig> feature = tree.getKey();
				int chance = tree.getValue();

				biome.addFeature(
						GenerationStep.Feature.VEGETAL_DECORATION,
						Biome.configureFeature(
								feature,
								FeatureConfig.DEFAULT,
								Decorator.CHANCE_HEIGHTMAP,
								new ChanceDecoratorConfig(chance)
						)
				);
			}

			// Add any minecraft (default) features

			for (DefaultFeature defaultFeature : defaultFeatures) {
				defaultFeature.add(biome);
			}

			// Add custom features that don't fit in the templates

			for (FeatureEntry feature : features) {
				biome.addFeature(feature.getStep(), feature.getFeature());
			}

			// Add Plant decoration features

			for (Map.Entry<BlockState, Integer> plant : plantFeatures.entrySet()) {
				biome.addFeature(
						GenerationStep.Feature.VEGETAL_DECORATION,
						Biome.configureFeature(Feature.GRASS, new GrassFeatureConfig(plant.getKey()), Decorator.COUNT_HEIGHTMAP_DOUBLE, new CountDecoratorConfig(plant.getValue())));
			}

			// Add Double Plant decoration features

			for (Map.Entry<BlockState, Integer> doublePlant : doublePlantFeatures.entrySet()) {
				biome.addFeature(
						GenerationStep.Feature.VEGETAL_DECORATION,
						Biome.configureFeature(Feature.DOUBLE_PLANT, new DoublePlantFeatureConfig(doublePlant.getKey()), Decorator.COUNT_HEIGHTMAP_32, new CountDecoratorConfig(doublePlant.getValue())));
			}


			return biome;
		}

		@Override
		public <SC extends SurfaceConfig> Builder configureSurfaceBuilder(SurfaceBuilder<SC> builder, SC config) {
			super.configureSurfaceBuilder(builder, config);

			return this;
		}

		@Override
		public Builder surfaceBuilder(ConfiguredSurfaceBuilder<?> surfaceBuilder) {
			super.surfaceBuilder(surfaceBuilder);

			return this;
		}

		@Override
		public Builder precipitation(Biome.Precipitation precipitation) {
			super.precipitation(precipitation);

			return this;
		}

		@Override
		public Builder category(Biome.Category category) {
			super.category(category);

			return this;
		}

		@Override
		public Builder depth(float depth) {
			super.depth(depth);

			return this;
		}

		@Override
		public Builder scale(float scale) {
			super.scale(scale);

			return this;
		}

		@Override
		public Builder temperature(float temperature) {
			super.temperature(temperature);

			return this;
		}

		@Override
		public Builder downfall(float downfall) {
			super.downfall(downfall);

			return this;
		}

		@Override
		public Builder waterColor(int color) {
			super.waterColor(color);

			return this;
		}

		@Override
		public Builder waterFogColor(int color) {
			super.waterFogColor(color);

			return this;
		}

		@Override
		public Builder parent(String parent) {
			super.parent(parent);

			return this;
		}

		public TerraformBiome.Builder addTreeFeature(Feature<DefaultFeatureConfig> feature, int numPerChunk) {
			this.treeFeatures.put(feature, numPerChunk);
			return this;
		}

		public TerraformBiome.Builder addRareTreeFeature(Feature<DefaultFeatureConfig> feature, int chance) {
			this.rareTreeFeatures.put(feature, chance);
			return this;
		}

		public TerraformBiome.Builder addGrassFeature(BlockState blockState, int count) {
			this.plantFeatures.put(blockState, count);
			return this;
		}

		public TerraformBiome.Builder addDoubleGrassFeature(BlockState blockState, int count) {
			this.doublePlantFeatures.put(blockState, count);
			return this;
		}

		public TerraformBiome.Builder addCustomFeature(GenerationStep.Feature step, ConfiguredFeature feature) {
			this.features.add(new FeatureEntry(step, feature));
			return this;
		}

		public TerraformBiome.Builder addSpawnEntry(SpawnEntry entry) {
			this.spawnEntries.add(entry);
			return this;
		}

		public TerraformBiome.Builder addStructureFeature(StructureFeature<DefaultFeatureConfig> feature) {
			return this.addStructureFeature(feature, FeatureConfig.DEFAULT);
		}

		public <FC extends FeatureConfig> TerraformBiome.Builder addStructureFeature(StructureFeature<FC> feature, FC config) {
			this.structureFeatures.put((StructureFeature)feature, config);
			return this;
		}

		public TerraformBiome.Builder addStructureFeatures(StructureFeature<DefaultFeatureConfig>... defaultStructureFeatures) {
			for (StructureFeature<DefaultFeatureConfig> feature : defaultStructureFeatures) {
				this.structureFeatures.put((StructureFeature) feature, FeatureConfig.DEFAULT);
			}
			return this;
		}

		public TerraformBiome.Builder addDefaultFeature(DefaultFeature feature) {
			defaultFeatures.add(feature);
			return this;
		}

		public TerraformBiome.Builder addDefaultFeatures(DefaultFeature... features) {
			defaultFeatures.addAll(Arrays.asList(features));
			return this;
		}

		public TerraformBiome.Builder grassColor(int color) {
			grassColor = color;
			return this;
		}

		public TerraformBiome.Builder foliageColor(int color) {
			foliageColor = color;
			return this;
		}
		
		public TerraformBiome.Builder spawnChance(int chance) {
			spawnChance = chance;
			return this;
		}

		public TerraformBiome.Builder addDefaultSpawnEntries() {
			this.addSpawnEntry(new Biome.SpawnEntry(EntityType.SHEEP, 12, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.PIG, 10, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.CHICKEN, 10, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.COW, 8, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.BAT, 10, 8, 8))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.SPIDER, 100, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.ZOMBIE, 95, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.ZOMBIE_VILLAGER, 5, 1, 1))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.SKELETON, 100, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.CREEPER, 100, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.SLIME, 100, 4, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.ENDERMAN, 10, 1, 4))
					.addSpawnEntry(new Biome.SpawnEntry(EntityType.WITCH, 5, 1, 1));
			return this;
		}
	}

	public static final class Template {
		private final Builder builder;

		public Template(Builder builder) {
			this.builder = builder;
			builder.template = true;
		}

		public Builder builder() {
			return new Builder(this.builder);
		}
	}
}
