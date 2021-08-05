package net.oskarstrom.dashloader.core.registry;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.oskarstrom.dashloader.api.Dashable;
import net.oskarstrom.dashloader.api.registry.DashRegistry;
import net.oskarstrom.dashloader.api.registry.FactoryConstructor;
import net.oskarstrom.dashloader.api.registry.RegistryStorage;

import java.util.List;

public abstract class RegistryStorageImpl<F, D extends Dashable<F>> implements RegistryStorage<F>, Dashable<List<F>> {
	private final Object2IntMap<F> deduplicationMap = new Object2IntOpenHashMap<>();
	private final DashRegistry registry;
	private final F[] unDashedObjects; // bound to F
	private D[] dashables; // bound to D
	private int pos = 0;

	public RegistryStorageImpl(DashRegistry registry) {
		this.registry = registry;
		this.unDashedObjects = null;
		//noinspection unchecked
		this.dashables = (D[]) new Dashable[1];
	}

	public RegistryStorageImpl(DashRegistry registry, D[] dashables) {
		this.registry = registry;
		this.dashables = dashables;
		//noinspection unchecked
		this.unDashedObjects = (F[]) new Object[dashables.length];
	}

	@Override
	public int add(F object) {
		if (deduplicationMap.containsKey(object))
			return deduplicationMap.getInt(object);
		final D dashObject = create(object, registry);

		dashables = ensureSize(dashables.length + 1, dashables);
		int pos = this.pos;
		dashables[pos] = dashObject;
		deduplicationMap.put(object, pos);
		this.pos++;
		return pos;
	}

	private D[] ensureSize(int size, D[] array) {
		if (array.length < size) {
			//noinspection unchecked
			D[] newArray = (D[]) new Dashable[array.length * 2];
			System.arraycopy(array, 0, newArray, 0, array.length);
			return newArray;
		}
		return array;
	}


	public abstract D create(F object, DashRegistry registry);


	@SuppressWarnings("unchecked")
	public D[] getDashables() {
		D[] trimmedArray = (D[]) new Dashable[pos + 1];
		if (pos >= 0) System.arraycopy(dashables, 0, trimmedArray, 0, pos + 1);
		return trimmedArray;
	}

	@Override
	public F get(int pointer) {
		//noinspection ConstantConditions
		return unDashedObjects[pointer];
	}

	@Override
	public List<F> toUndash(DashRegistry registry) {
		if (dashables == null || dashables.length == 0) {
			throw new IllegalStateException("Dashables are not available.");
		}
		return null;
	}

	static class SimpleRegistryImpl<F, D extends Dashable<F>> extends RegistryStorageImpl<F, D> {
		private final FactoryConstructor<F, D> constructor;

		public SimpleRegistryImpl(FactoryConstructor<F, D> constructor, DashRegistry registry) {
			super(registry);
			this.constructor = constructor;
		}

		@Override
		public D create(F object, DashRegistry registry) {
			return constructor.create(object, registry);
		}
	}

	static class FactoryRegistryImpl<F, D extends Dashable<F>> extends RegistryStorageImpl<F, D> {
		private final Object2ObjectMap<Class<F>, FactoryConstructor<F, D>> constructor;

		public FactoryRegistryImpl(Object2ObjectMap<Class<F>, FactoryConstructor<F, D>> constructor, DashRegistry registry) {
			super(registry);
			this.constructor = constructor;
		}

		@Override
		public D create(F object, DashRegistry registry) {
			final FactoryConstructor<F, D> fdFactoryConstructor = constructor.get(object);
			if (fdFactoryConstructor == null) {
				//TODO error handling
				throw new IllegalStateException();
			}
			return fdFactoryConstructor.create(object, registry);
		}
	}
}
