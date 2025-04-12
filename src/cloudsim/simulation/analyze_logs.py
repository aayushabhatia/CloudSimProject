import matplotlib.pyplot as plt
import re
import os

# Path to the report file
report_path = os.path.join(os.path.dirname(__file__), "../../../scheduling_comparison_report.txt")

# Read the file
with open(report_path, "r") as f:
    content = f.read()

# Extract runtimes using regular expressions
time_shared_runtime = re.search(r"Time-Shared Runtime:\s*([\d.]+)s", content)
space_shared_runtime = re.search(r"Space-Shared Runtime:\s*([\d.]+)s", content)

# Convert to float if found
time_shared_runtime = float(time_shared_runtime.group(1)) if time_shared_runtime else None
space_shared_runtime = float(space_shared_runtime.group(1)) if space_shared_runtime else None

print("TimeShared Execution Time:", time_shared_runtime)
print("SpaceShared Execution Time:", space_shared_runtime)

# Plotting
if time_shared_runtime is not None and space_shared_runtime is not None:
    strategies = ["Time-Shared", "Space-Shared"]
    execution_times = [time_shared_runtime, space_shared_runtime]

    plt.bar(strategies, execution_times, color=['skyblue', 'salmon'])
    plt.title("Execution Time Comparison of Scheduling Strategies")
    plt.ylabel("Execution Time (seconds)")
    plt.xlabel("Scheduling Strategy")
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.tight_layout()
    plt.savefig("execution_time_comparison.png")
    plt.show()
else:
    print("Error: Could not extract execution times from the report.")
