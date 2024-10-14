import os
import csv
import re

with open('random_2024_androzoo.lst', 'r') as hash_file:
    hashes = [line.strip() for line in hash_file if line.strip()]

with open('result.csv', 'w', newline='') as csvfile:
    fieldnames = [
        'Hash',
        'WithDC_CALL_GRAPH_SIZE',
        'WithoutDC_CALL_GRAPH_SIZE',
        'WithDC_Time',
        'WithoutDC_Time'
    ]
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()

    for h in hashes:
        withdc_size = None
        withoutdc_size = None
        withdc_time = None
        withoutdc_time = None

        withdc_path = os.path.join('flowdroid_runs', f'WithDC-{h}.apk')
        withoutdc_path = os.path.join('flowdroid_runs', f'WithoutDC-{h}.apk')

        size_pattern = re.compile(r'CALL_GRAPH_SIZE:(\d+)')
        time_pattern = re.compile(
            r'(\d+\.\d+)user\s+(\d+\.\d+)system\s+([\d:]+\.\d+)elapsed')

        def time_to_seconds(time_str):
            match = re.match(r'(\d+):([\d\.]+)', time_str)
            if match:
                minutes = int(match.group(1))
                seconds = float(match.group(2))
                total_seconds = minutes * 60 + seconds
                return total_seconds
            else:
                try:
                    return float(time_str)
                except ValueError:
                    return None

        if os.path.isfile(withdc_path):
            with open(withdc_path, 'r') as file:
                content = file.read()
                match_size = size_pattern.search(content)
                if match_size:
                    withdc_size = match_size.group(1)
                match_time = time_pattern.search(content)
                if match_time:
                    elapsed_time_str = match_time.group(3).replace('elapsed', '').strip()
                    total_seconds = time_to_seconds(elapsed_time_str)
                    withdc_time = total_seconds

        if os.path.isfile(withoutdc_path):
            with open(withoutdc_path, 'r') as file:
                content = file.read()
                match_size = size_pattern.search(content)
                if match_size:
                    withoutdc_size = match_size.group(1)
                match_time = time_pattern.search(content)
                if match_time:
                    elapsed_time_str = match_time.group(3).replace('elapsed', '').strip()
                    total_seconds = time_to_seconds(elapsed_time_str)
                    withoutdc_time = total_seconds

        if any([withdc_size, withoutdc_size, withdc_time, withoutdc_time]):
            writer.writerow({
                'Hash': h,
                'WithDC_CALL_GRAPH_SIZE': withdc_size or '',
                'WithoutDC_CALL_GRAPH_SIZE': withoutdc_size or '',
                'WithDC_Time': withdc_time or '',
                'WithoutDC_Time': withoutdc_time or ''
            })