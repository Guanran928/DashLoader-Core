package dev.quantumfusion.dashloader.core.registry.chunk.write;

import dev.quantumfusion.dashloader.core.Dashable;
import dev.quantumfusion.dashloader.core.api.DashConstructor;
import dev.quantumfusion.dashloader.core.registry.DashRegistryWriter;
import dev.quantumfusion.dashloader.core.registry.WriteFailCallback;
import dev.quantumfusion.dashloader.core.registry.chunk.data.AbstractDataChunk;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MultiChunkWriter<R, D extends Dashable<R>> extends ChunkWriter<R, D> {
	private final Object2ObjectMap<Class<?>, DashConstructor<R, D>> mappings;
	private final Class<?> dashType;

	private final List<D> dashableList = new ArrayList<>();
	private final WriteFailCallback<R, D> callback;

	public MultiChunkWriter(byte pos, DashRegistryWriter registry, Object2ObjectMap<Class<?>, DashConstructor<R, D>> mappings, Class<?> dashType, WriteFailCallback<R, D> callback) {
		super(pos, registry);
		this.mappings = mappings;
		this.dashType = dashType;
		this.callback = callback;
	}

	@Override
	public int add(R object) {
		final int pos = dashableList.size();
		final DashConstructor<R, D> rdDashConstructor = mappings.get(object.getClass());
		if (rdDashConstructor == null)
			dashableList.add(callback.fail(object, registry));
		else dashableList.add(rdDashConstructor.invoke(object, registry));
		return pos;
	}

	@Override
	public Collection<Class<?>> getClasses() {
		return mappings.keySet();
	}

	@Override
	public Collection<Class<?>> getDashClasses() {
		return mappings.values().stream().map(constructor -> constructor.dashClass).collect(Collectors.toList());
	}

	@Override
	public AbstractDataChunk<R, D> exportData() {
		return export(dashType, dashableList);
	}
}
