package dev.quantumfusion.dashloader.core.registry.chunk.write;

import dev.quantumfusion.dashloader.core.DashObjectClass;
import dev.quantumfusion.dashloader.core.Dashable;
import dev.quantumfusion.dashloader.core.registry.RegistryWriter;
import dev.quantumfusion.dashloader.core.registry.chunk.data.AbstractDataChunk;
import dev.quantumfusion.dashloader.core.registry.chunk.data.StagedDataChunk;
import dev.quantumfusion.dashloader.core.registry.factory.DashFactory;
import dev.quantumfusion.dashloader.core.thread.IndexedArrayMapTask;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class StagedWriteChunk<R, D extends Dashable<R>> extends AbstractWriteChunk<R, D> {
	private final Object2ObjectLinkedOpenHashMap<Class<?>, List<IndexedArrayMapTask.IndexedArrayEntry<D>>> dashList = new Object2ObjectLinkedOpenHashMap<>();
	private int currentPos = 0;

	public StagedWriteChunk(byte pos, String name, RegistryWriter writer, List<DashObjectClass<R, D>> dashObjects, DashFactory<R, D> factory) {
		super(pos, name, writer, factory, dashObjects);
		// this is ordered from creation
		dashObjects.forEach(dashObject -> dashList.put(dashObject.getDashClass(), new ArrayList<>()));
	}

	@Override
	public int add(R raw) {
		final D created = factory.create(raw, writer);
		dashList.computeIfAbsent(created.getClass(), c -> new ArrayList<>()) // just in case the fallback returns an unknown model
				.add(new IndexedArrayMapTask.IndexedArrayEntry<>(created, currentPos));
		return currentPos++;
	}

	@Override
	public AbstractDataChunk<R, D> exportData() {
		IndexedArrayMapTask.IndexedArrayEntry<D>[][] out = new IndexedArrayMapTask.IndexedArrayEntry[dashList.size()][];

		int i = 0;
		int size = 0;
		for (List<IndexedArrayMapTask.IndexedArrayEntry<D>> value : dashList.values()) {
			out[i++] = value.toArray(IndexedArrayMapTask.IndexedArrayEntry[]::new);
			size += value.size();
		}

		return new StagedDataChunk<>(pos, name, out, size);
	}
}
