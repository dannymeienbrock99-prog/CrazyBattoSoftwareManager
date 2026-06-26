using System;
using System.Diagnostics;
using System.Management;
using System.Threading.Tasks;

namespace CrazyBattoSoftwareManager.App.Services
{
    public class SystemInfoService
    {
        private PerformanceCounter _cpuCounter;
        private PerformanceCounter _ramCounter;

        public SystemInfoService()
        {
            InitializePerformanceCounters();
        }

        private void InitializePerformanceCounters()
        {
            try
            {
                _cpuCounter = new PerformanceCounter("Processor", "% Processor Time", "_Total", true);
                _ramCounter = new PerformanceCounter("Memory", "% Committed Bytes In Use", true);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Performance Counter Init Error: {ex.Message}");
            }
        }

        /// <summary>
        /// Get current CPU usage percentage
        /// </summary>
        public float GetCpuUsage()
        {
            try
            {
                return _cpuCounter?.NextValue() ?? 0f;
            }
            catch
            {
                return 0f;
            }
        }

        /// <summary>
        /// Get current RAM usage percentage
        /// </summary>
        public float GetRamUsage()
        {
            try
            {
                return _ramCounter?.NextValue() ?? 0f;
            }
            catch
            {
                return 0f;
            }
        }

        /// <summary>
        /// Get GPU temperature (requires GPU monitoring driver)
        /// </summary>
        public float GetGpuTemperature()
        {
            try
            {
                using (var searcher = new ManagementObjectSearcher(@"\\.\root\wmi", "SELECT * FROM MSAcpi_ThermalZoneTemperature"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        var temp = Convert.ToUInt32(obj["CurrentTemperature"]);
                        return (temp - 2732) / 10f; // Convert to Celsius
                    }
                }
            }
            catch
            {
                return 0f;
            }
            return 0f;
        }

        /// <summary>
        /// Get CPU temperature (requires thermal sensor)
        /// </summary>
        public float GetCpuTemperature()
        {
            try
            {
                using (var searcher = new ManagementObjectSearcher(@"\\.\root\wmi", "SELECT * FROM MSAcpi_ThermalZoneTemperature"))
                {
                    int index = 0;
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        if (index == 0) // First thermal zone is typically CPU
                        {
                            var temp = Convert.ToUInt32(obj["CurrentTemperature"]);
                            return (temp - 2732) / 10f;
                        }
                        index++;
                    }
                }
            }
            catch
            {
                return 0f;
            }
            return 0f;
        }

        /// <summary>
        /// Get available disk space (in GB)
        /// </summary>
        public (float Used, float Total) GetDiskSpace()
        {
            try
            {
                var drives = System.IO.DriveInfo.GetDrives();
                if (drives.Length > 0)
                {
                    var cDrive = drives[0];
                    float total = cDrive.TotalSize / (1024f * 1024f * 1024f);
                    float available = cDrive.AvailableFreeSpace / (1024f * 1024f * 1024f);
                    float used = total - available;
                    return (used, total);
                }
            }
            catch { }
            return (0, 0);
        }

        /// <summary>
        /// Get RAM info (in GB)
        /// </summary>
        public (float Available, float Total) GetRamInfo()
        {
            try
            {
                using (var mc = new ManagementClass("Win32_ComputerSystem"))
                {
                    using (var moc = mc.GetInstances())
                    {
                        foreach (ManagementObject mo in moc)
                        {
                            var total = Convert.ToUInt64(mo["TotalPhysicalMemory"]) / (1024f * 1024f * 1024f);
                            
                            using (var ram = new ManagementObjectSearcher("SELECT AvailablePhysicalMemory FROM Win32_OperatingSystem"))
                            {
                                foreach (ManagementObject o in ram.Get())
                                {
                                    var available = Convert.ToUInt64(o["AvailablePhysicalMemory"]) / (1024f * 1024f * 1024f);
                                    return (available, total);
                                }
                            }
                        }
                    }
                }
            }
            catch { }
            return (0, 0);
        }

        /// <summary>
        /// Get motherboard and system info
        /// </summary>
        public SystemInfo GetSystemInfo()
        {
            var info = new SystemInfo();

            try
            {
                // Get motherboard info
                using (var searcher = new ManagementObjectSearcher("SELECT * FROM Win32_BaseBoard"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        info.MotherboardManufacturer = obj["Manufacturer"]?.ToString() ?? "Unknown";
                        info.MotherboardModel = obj["Product"]?.ToString() ?? "Unknown";
                    }
                }

                // Get CPU info
                using (var searcher = new ManagementObjectSearcher("SELECT * FROM Win32_Processor"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        info.ProcessorName = obj["Name"]?.ToString() ?? "Unknown";
                        info.CoreCount = Convert.ToInt32(obj["NumberOfCores"] ?? 0);
                        info.ThreadCount = Convert.ToInt32(obj["ThreadCount"] ?? 0);
                    }
                }

                // Get GPU info
                using (var searcher = new ManagementObjectSearcher("SELECT * FROM Win32_VideoController"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        info.GpuName = obj["Name"]?.ToString() ?? "Unknown";
                        info.GpuMemory = (Convert.ToUInt64(obj["AdapterRAM"] ?? 0) / (1024f * 1024f * 1024f)).ToString("F2");
                        break; // Primary GPU
                    }
                }

                // Get OS info
                using (var searcher = new ManagementObjectSearcher("SELECT * FROM Win32_OperatingSystem"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        info.OsName = obj["Caption"]?.ToString() ?? "Unknown";
                        info.OsVersion = obj["Version"]?.ToString() ?? "Unknown";
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"SystemInfo Error: {ex.Message}");
            }

            return info;
        }
    }

    public class SystemInfo
    {
        public string MotherboardManufacturer { get; set; } = "Unknown";
        public string MotherboardModel { get; set; } = "Unknown";
        public string ProcessorName { get; set; } = "Unknown";
        public int CoreCount { get; set; }
        public int ThreadCount { get; set; }
        public string GpuName { get; set; } = "Unknown";
        public string GpuMemory { get; set; } = "Unknown";
        public string OsName { get; set; } = "Unknown";
        public string OsVersion { get; set; } = "Unknown";
    }
}
