# Particle collision test

1. Open `/mgc` → Debug → Diagnostics. Enable Diagnostic Logging, expand it, then enable Particle Collision Profiling. Apply the change. Other diagnostic categories can be disabled to keep the log readable.
2. Keep particle settings and the rate of parries consistent. Test in open air well away from blocks, above a flat floor, and beside a wall/floor corner. Use the same type of parry in each location.
3. Let the test warm up for a few reports, then collect two or three reports per location. Each complete report covers 100 advancing game ticks, normally about five seconds. Paused ticks do not advance the reporting window.
4. Attach `logs/latest.log` to your report and note which reports belong to each location. Report lines begin with `Particle collisions:`. Disable profiling after testing.

Flying Sparks, Spark Debris and Spark Tracers are reported separately. To compare their individual collision costs, keep their settings fixed; optionally test one effect at a time by setting the other effects' base amounts to zero temporarily.

## Reading the results

- `slow`: checks that use the small collision box for slower movement.
- `fast_first`: the first path check for fast movement.
- `fast_second`: the extra path check after a fast impact, when remaining movement needs checking.
- `hits`: checks whose movement was blocked. These are not counts of unique blocks or particles.
- `avg_us`: average microseconds per check in that group.
- `avg_ms_per_tick` and `peak_ms_per_tick`: average and largest accumulated collision query time during one tick in this window.
- `second_check_pct`: how often a first fast check was followed by a second check.
- `peak_heads`: largest observed active head count, including heads fading at rest.
- `wall_s`: actual elapsed time for the reporting window.

The timings include query setup and measurement overhead, but exclude bounce response, particle drawing and other mod work. Profile totals therefore cannot establish overall FPS or prove which particle implementation is faster overall. Compare frame performance with profiling turned off. Changing worlds clears the measurements; disabling profiling reports an unfinished window when possible.
