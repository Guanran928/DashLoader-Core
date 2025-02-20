package dev.quantumfusion.dashloader.core.thread;

import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;

@SuppressWarnings("FinalMethodInFinalClass")
public final class ArrayMapTask<I, O> extends ForkJoinTask<Void> {
	private final int threshold;
	private final int start;
	private final int stop;
	private final I[] inArray;
	private final O[] outArray;
	private final Function<I, O> function;

	private ArrayMapTask(I[] inArray, O[] outArray, Function<I, O> function, int threshold, int start, int stop) {
		this.threshold = threshold;
		this.start = start;
		this.stop = stop;
		this.inArray = inArray;
		this.outArray = outArray;
		this.function = function;
	}

	public ArrayMapTask(I[] inArray, O[] outArray, Function<I, O> function) {
		this.start = 0;
		this.stop = inArray.length;
		this.threshold = ThreadHandler.calcThreshold(stop);
		this.inArray = inArray;
		this.outArray = outArray;
		this.function = function;
	}

	@Override
	protected final boolean exec() {
		final int size = stop - start;
		if (size < threshold) {
			for (int i = start; i < stop; i++)
				outArray[i] = function.apply(inArray[i]);
		} else {
			final int middle = start + (size / 2);
			invokeAll(new ArrayMapTask<>(inArray, outArray, function, threshold, start, middle),
					  new ArrayMapTask<>(inArray, outArray, function, threshold, middle, stop));
		}
		return true;
	}

	public final Void getRawResult() {
		return null;
	}

	protected final void setRawResult(Void mustBeNull) {
	}
}
