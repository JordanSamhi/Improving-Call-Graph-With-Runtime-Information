import csv
import statistics
import matplotlib.pyplot as plt

withdc_sizes = []
withoutdc_sizes = []
withdc_times = []
withoutdc_times = []

with open('result.csv', 'r') as csvfile:
    reader = csv.DictReader(csvfile)
    
    for row in reader:
        try:
            withdc_size = int(row['WithDC_CALL_GRAPH_SIZE'])
            withoutdc_size = int(row['WithoutDC_CALL_GRAPH_SIZE'])
            withdc_time = float(row['WithDC_Time'])
            withoutdc_time = float(row['WithoutDC_Time'])
        except (ValueError, KeyError):
            continue

        withdc_sizes.append(withdc_size)
        withoutdc_sizes.append(withoutdc_size)
        withdc_times.append(withdc_time)
        withoutdc_times.append(withoutdc_time)

withdc_size_average = sum(withdc_sizes) / len(withdc_sizes) if withdc_sizes else 0
withoutdc_size_average = sum(withoutdc_sizes) / len(withoutdc_sizes) if withoutdc_sizes else 0

withdc_size_median = statistics.median(withdc_sizes) if withdc_sizes else 0
withoutdc_size_median = statistics.median(withoutdc_sizes) if withoutdc_sizes else 0

withdc_time_average = sum(withdc_times) / len(withdc_times) if withdc_times else 0
withoutdc_time_average = sum(withoutdc_times) / len(withoutdc_times) if withoutdc_times else 0

withdc_time_median = statistics.median(withdc_times) if withdc_times else 0
withoutdc_time_median = statistics.median(withoutdc_times) if withoutdc_times else 0

print("Call Graph Sizes with DC:")
print(f"Average CALL_GRAPH_SIZE: {withdc_size_average}")
print(f"Median CALL_GRAPH_SIZE: {withdc_size_median}\n")

print("Call Graph Sizes without DC:")
print(f"Average CALL_GRAPH_SIZE: {withoutdc_size_average}")
print(f"Median CALL_GRAPH_SIZE: {withoutdc_size_median}\n")

print("Elapsed Times with DC:")
print(f"Average Time Elapsed: {withdc_time_average} seconds")
print(f"Median Time Elapsed: {withdc_time_median} seconds\n")

print("Elapsed Times without DC:")
print(f"Average Time Elapsed: {withoutdc_time_average} seconds")
print(f"Median Time Elapsed: {withoutdc_time_median} seconds\n")

sizes_data = [withdc_sizes, withoutdc_sizes]
times_data = [withdc_times, withoutdc_times]

positions = [0.6, 0.9]
heights = 0.10
colors = ['lightblue', 'lightgreen']

fig1, ax1 = plt.subplots(figsize=(8, 2))

bp_sizes = ax1.boxplot(
    sizes_data,
    positions=positions,
    widths=heights,
    vert=False,
    patch_artist=True,
    showfliers=False
)

for patch, color in zip(bp_sizes['boxes'], colors):
    patch.set_facecolor(color)

ax1.set_yticks(positions)
ax1.set_yticklabels(['With DC', 'Without DC'])
ax1.set_ylim(0.5, 1.0)
ax1.set_xlabel('Call Graph Size')

plt.tight_layout()

plt.show()

fig2, ax2 = plt.subplots(figsize=(8, 2))

bp_times = ax2.boxplot(
    times_data,
    positions=positions,
    widths=heights,
    vert=False,
    patch_artist=True,
    showfliers=False
)

for patch, color in zip(bp_times['boxes'], colors):
    patch.set_facecolor(color)

ax2.set_yticks(positions)
ax2.set_yticklabels(['With DC', 'Without DC'])
ax2.set_ylim(0.5, 1.0)
ax2.set_xlabel('Time Elapsed (seconds)')

plt.tight_layout()

plt.show()