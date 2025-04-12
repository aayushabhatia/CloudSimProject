import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.animation import FuncAnimation
import time
import re

# Paths
space_path = "/app/SpaceShared_simulation_log.txt"
time_path = "/app/TimeShared_simulation_log.txt"

# Load cloudlet data from log file
def load_data(filepath):
    with open(filepath, "r") as f:
        lines = f.readlines()

    cloudlets = []
    reading = False
    for line in lines:
        if "CLOUDLET EXECUTION RESULTS:" in line:
            reading = True
            continue
        if reading:
            if line.strip() == "":
                break
            match = re.match(r"(\d+)\s+SUCCESS\s+(\d+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)", line)
            if match:
                cid, vmid, time_exec, start, finish = match.groups()
                cloudlets.append({
                    "id": int(cid),
                    "vm": int(vmid),
                    "exec_time": float(time_exec),
                    "start": float(start),
                    "finish": float(finish),
                    "status": "WAITING"
                })
    return cloudlets

# Visualize one strategy
def run_animation(strategy_name, filepath, num_vms=4):
    cloudlets = load_data(filepath)
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.set_xlim(0, 10)
    ax.set_ylim(-1, num_vms + 1)
    ax.set_title(f"{strategy_name} VM Scheduling Animation")
    ax.set_xlabel("Time")
    ax.set_ylabel("VM ID")

    vm_lanes = [i for i in range(num_vms)]
    cloudlet_rects = []
    progress_bars = []

    for c in cloudlets:
        # Initial position (waiting area)
        rect = patches.Rectangle((-1, -1), 0.6, 0.4, linewidth=1, edgecolor='black', facecolor='lightgray')
        ax.add_patch(rect)
        cloudlet_rects.append(rect)

        # Progress bar overlay
        bar = patches.Rectangle((-1, -1), 0, 0.4, linewidth=0, facecolor='green')
        ax.add_patch(bar)
        progress_bars.append(bar)

    time_text = ax.text(0.02, 1.02, '', transform=ax.transAxes)

    def update(frame):
        sim_time = frame / 10.0  # speed multiplier
        time_text.set_text(f"Simulated Time: {sim_time:.1f}s")
        for i, c in enumerate(cloudlets):
            rect = cloudlet_rects[i]
            bar = progress_bars[i]
            vm_y = c["vm"]

            if sim_time < c["start"]:
                rect.set_xy((-1, -1))  # Not started
                bar.set_xy((-1, -1))
            elif c["start"] <= sim_time <= c["finish"]:
                # Move to VM
                x = 2 + (sim_time - c["start"]) / c["exec_time"] * 4  # progress x
                rect.set_xy((x, vm_y))
                bar.set_xy((x, vm_y))
                bar.set_width(0.6 * (sim_time - c["start"]) / c["exec_time"])
                bar.set_height(0.4)
            elif sim_time > c["finish"]:
                # Mark as completed
                rect.set_xy((6, vm_y))
                bar.set_xy((6, vm_y))
                bar.set_width(0.6)
                bar.set_height(0.4)
                rect.set_facecolor('lightgreen')

        return cloudlet_rects + progress_bars + [time_text]

    ani = FuncAnimation(fig, update, frames=range(0, int(max(c["finish"] for c in cloudlets) * 10 + 20)), interval=100, blit=True)
    # Save the animation
    ani.save(f"/app/output/{strategy_name}_animation.mp4", writer='ffmpeg', fps=1)
    print(f"{strategy_name} animation saved as /app/output/{strategy_name}_animation.mp4")


    # plt.show()

# Run both animations one after another
print("Running SpaceShared animation...")
run_animation("SpaceShared", space_path)

print("Running TimeShared animation...")
run_animation("TimeShared", time_path)
