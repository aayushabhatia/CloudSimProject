# Dockerfile

FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Install ffmpeg for video generation
RUN apt-get update && apt-get install -y ffmpeg

# Copy requirements and install dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy the full source code
COPY src/ ./src/
COPY scheduling_comparison_report.txt ./scheduling_comparison_report.txt
COPY SpaceShared_simulation_log.txt ./SpaceShared_simulation_log.txt
COPY TimeShared_simulation_log.txt ./TimeShared_simulation_log.txt


# Default command: Run analyze_logs.py
CMD ["python", "src/cloudsim/simulation/animate_execution.py"]


