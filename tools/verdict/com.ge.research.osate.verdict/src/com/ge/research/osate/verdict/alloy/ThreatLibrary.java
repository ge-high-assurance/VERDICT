package com.ge.research.osate.verdict.alloy;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.ge.research.osate.verdict.dsl.verdict.ThreatDatabase;
import com.ge.research.osate.verdict.dsl.verdict.ThreatDefense;
import com.ge.research.osate.verdict.dsl.verdict.ThreatModel;

/**
 * Container for loaded threat model information.
 * 
 * This class supports streams and collectors and stuff.
 * (See ThreatModelParser for usage.)
 */
public class ThreatLibrary {
	private final Map<String, ThreatDatabase> databases;
	private final Map<String, ThreatModel> threats;
	private final Map<String, ThreatDefense> defenses;
	
	/*
	 * The confusing bit for the collectors is that we use the
	 * same class (ThreatLibrary) for both the mutating one
	 * and the immutable one that we produce at the end.
	 * One constructor (no arguments) creates a mutable one
	 * and is only used internally. Another constructor (ThreatLibrary
	 * as parameter) is only used for capping off with an immutable
	 * instance at the end.
	 * 
	 * It works so should be no need to mess with it.
	 * 
	 * In retrospect it might have been better to do this in
	 * an imperative rather than functional style though.
	 * I guess Java isn't a functional language after all.
	 */
	
	private ThreatLibrary() {
		databases = new LinkedHashMap<>();
		threats = new LinkedHashMap<>();
		defenses = new LinkedHashMap<>();
	}
	
	private ThreatLibrary(ThreatLibrary other) {
		databases = Collections.unmodifiableMap(other.databases);
		threats = Collections.unmodifiableMap(other.threats);
		defenses = Collections.unmodifiableMap(other.defenses);
	}
	
	private <T> Map<String, T> idMapper(List<T> list, Function<T, String> idFunction) {
		return Collections.unmodifiableMap(
				list.stream().collect(Collectors.toMap(
						idFunction, Function.identity(),
						(u, v) -> {
							throw new IllegalStateException(
									String.format("Duplicate key %s", u));
						}, LinkedHashMap::new)));
	}
	
	private ThreatLibrary(
			Map<String, ThreatDatabase> databases,
			Map<String, ThreatModel> threats,
			Map<String, ThreatDefense> defenses) {
		
		this.databases = Collections.unmodifiableMap(databases);
		this.threats = Collections.unmodifiableMap(threats);
		this.defenses = Collections.unmodifiableMap(defenses);
	}
	
	public ThreatLibrary(
			List<ThreatDatabase> databases,
			List<ThreatModel> threats,
			List<ThreatDefense> defenses) {
		
		this.databases = idMapper(databases, ThreatDatabase::getId);
		this.threats = idMapper(threats, ThreatModel::getId);
		this.defenses = idMapper(defenses, ThreatDefense::getName);
	}
	
	private static <K, V> Map<K, V> mapUnion(Map<K, V> mapA, Map<K, V> mapB) {
		Map<K, V> concat = new LinkedHashMap<>();
		concat.putAll(mapA);
		concat.putAll(mapB);
		return concat;
	}
	
	public static Collector<ThreatLibrary, ?, ThreatLibrary> collector() {
		return Collector.of(
				() -> new ThreatLibrary(), // mutable
				(acc, library) -> {
					acc.databases.putAll(library.databases);
					acc.threats.putAll(library.threats);
					acc.defenses.putAll(library.defenses);
				},
				(lib1, lib2) -> new ThreatLibrary(
						mapUnion(lib1.databases, lib2.databases),
						mapUnion(lib1.threats, lib2.threats),
						mapUnion(lib1.defenses, lib2.defenses)),
				lib -> new ThreatLibrary(lib) // make immutable
			);
	}
	
	public Collection<ThreatDatabase> getDatabases() {
		return databases.values();
	}
	
	public Collection<ThreatModel> getThreats() {
		return threats.values();
	}
	
	public Collection<ThreatDefense> getDefenses() {
		return defenses.values();
	}
	
	public Map<String, ThreatDatabase> getDatabaseMap() {
		return databases;
	}
	
	public Map<String, ThreatModel> getThreatMap() {
		return threats;
	}
	
	public Map<String, ThreatDefense> getDefenseMap() {
		return defenses;
	}
}
