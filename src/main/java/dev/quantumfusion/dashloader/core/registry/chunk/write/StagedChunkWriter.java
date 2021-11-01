package dev.quantumfusion.dashloader.core.registry.chunk.write;

import dev.quantumfusion.dashloader.core.Dashable;
import dev.quantumfusion.dashloader.core.api.DashConstructor;
import dev.quantumfusion.dashloader.core.registry.DashRegistryWriter;
import dev.quantumfusion.dashloader.core.registry.WriteFailCallback;
import dev.quantumfusion.dashloader.core.registry.chunk.data.AbstractDataChunk;
import dev.quantumfusion.dashloader.core.registry.chunk.data.StagedDataChunk;
import dev.quantumfusion.dashloader.core.ui.DashLoaderProgress;
import dev.quantumfusion.dashloader.core.util.DashableEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class StagedChunkWriter<R, D extends Dashable<R>> extends ChunkWriter<R, D> {
	private final Object2ObjectMap<Class<?>, StageInfo<R, D>> mappings;
	private final List<DashableEntry<D>>[] dashableList;
	private final List<DashableEntry<D>> failed = new ArrayList<>();
	private final WriteFailCallback<R, D> callback;
	private final Class<?> dashType;
	private int objectPos = 0;

	public StagedChunkWriter(byte pos, DashRegistryWriter registry, int stages, Object2ObjectMap<Class<?>, StageInfo<R, D>> mappings, WriteFailCallback<R, D> callback, Class<?> dashType) {
		super(pos, registry);
		this.mappings = mappings;
		//noinspection unchecked
		this.dashableList = new List[stages];
		this.callback = callback;
		this.dashType = dashType;
		for (int i = 0; i < this.dashableList.length; i++)
			dashableList[i] = new ArrayList<>();
	}

	@Override
	public int add(R object) {
		var stageInfo = mappings.get(object.getClass());
		if (stageInfo == null)
			failed.add(new DashableEntry<>(objectPos, callback.fail(object, registry)));
		else {
			final D dashObject = stageInfo.constructor.invoke(object, registry);
			dashableList[stageInfo.stage].add(new DashableEntry<>(objectPos, dashObject));
		}
		return objectPos++;
	}

	@Override
	public Collection<Class<?>> getClasses() {
		return mappings.keySet();
	}

	@Override
	public Collection<Class<?>> getDashClasses() {
		return mappings.values().stream().map(constructor -> constructor.constructor.dashClass).collect(Collectors.toList());
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractDataChunk<R, D> exportData() {
		var name = dashType.getSimpleName();
		DashLoaderProgress.PROGRESS.setCurrentSubtask("Exporting " + name, dashableList.length + failed.size());
		var out = new DashableEntry[dashableList.length][];

		for (int i = 0; i < dashableList.length; i++) {
			out[i] = dashableList[i].toArray(DashableEntry[]::new);
			DashLoaderProgress.PROGRESS.completedSubTask();
		}

		var outFailed = new DashableEntry[failed.size()];

		for (int i = 0; i < failed.size(); i++) {
			outFailed[i] = failed.get(i);
			DashLoaderProgress.PROGRESS.completedSubTask();
		}

		return new StagedDataChunk<R, D>(pos, name, out, outFailed, objectPos);
	}


	public record StageInfo<R, D extends Dashable<R>>(DashConstructor<R, D> constructor, int stage) {
	}
}
