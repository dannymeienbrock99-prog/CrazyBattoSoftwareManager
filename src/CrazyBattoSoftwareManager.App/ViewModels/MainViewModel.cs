using System.ComponentModel;
using System.Runtime.CompilerServices;
using CrazyBattoSoftwareManager.App.Services;

namespace CrazyBattoSoftwareManager.App.ViewModels
{
    public class MainViewModel : INotifyPropertyChanged
    {
        private readonly SystemInfoService _systemInfoService;

        private float _cpuUsage;
        private float _ramUsage;
        private float _cpuTemp;
        private float _gpuTemp;
        private float _diskUsed;
        private float _diskTotal;
        private float _ramAvailable;
        private float _ramTotal;
        private string _motherboard = "Laden...";
        private string _processor = "Laden...";
        private string _gpu = "Laden...";
        private string _osName = "Laden...";

        public event PropertyChangedEventHandler PropertyChanged;

        public MainViewModel()
        {
            _systemInfoService = new SystemInfoService();
            LoadSystemInfo();
        }

        public float CpuUsage
        {
            get => _cpuUsage;
            set { if (_cpuUsage != value) { _cpuUsage = value; OnPropertyChanged(); } }
        }

        public float RamUsage
        {
            get => _ramUsage;
            set { if (_ramUsage != value) { _ramUsage = value; OnPropertyChanged(); } }
        }

        public float CpuTemp
        {
            get => _cpuTemp;
            set { if (_cpuTemp != value) { _cpuTemp = value; OnPropertyChanged(); } }
        }

        public float GpuTemp
        {
            get => _gpuTemp;
            set { if (_gpuTemp != value) { _gpuTemp = value; OnPropertyChanged(); } }
        }

        public float DiskUsed
        {
            get => _diskUsed;
            set { if (_diskUsed != value) { _diskUsed = value; OnPropertyChanged(); } }
        }

        public float DiskTotal
        {
            get => _diskTotal;
            set { if (_diskTotal != value) { _diskTotal = value; OnPropertyChanged(); } }
        }

        public float RamAvailable
        {
            get => _ramAvailable;
            set { if (_ramAvailable != value) { _ramAvailable = value; OnPropertyChanged(); } }
        }

        public float RamTotal
        {
            get => _ramTotal;
            set { if (_ramTotal != value) { _ramTotal = value; OnPropertyChanged(); } }
        }

        public string Motherboard
        {
            get => _motherboard;
            set { if (_motherboard != value) { _motherboard = value; OnPropertyChanged(); } }
        }

        public string Processor
        {
            get => _processor;
            set { if (_processor != value) { _processor = value; OnPropertyChanged(); } }
        }

        public string Gpu
        {
            get => _gpu;
            set { if (_gpu != value) { _gpu = value; OnPropertyChanged(); } }
        }

        public string OsName
        {
            get => _osName;
            set { if (_osName != value) { _osName = value; OnPropertyChanged(); } }
        }

        public void RefreshSystemMetrics()
        {
            CpuUsage = _systemInfoService.GetCpuUsage();
            RamUsage = _systemInfoService.GetRamUsage();
            CpuTemp = _systemInfoService.GetCpuTemperature();
            GpuTemp = _systemInfoService.GetGpuTemperature();

            var diskInfo = _systemInfoService.GetDiskSpace();
            DiskUsed = diskInfo.Used;
            DiskTotal = diskInfo.Total;

            var ramInfo = _systemInfoService.GetRamInfo();
            RamAvailable = ramInfo.Available;
            RamTotal = ramInfo.Total;
        }

        private void LoadSystemInfo()
        {
            var sysInfo = _systemInfoService.GetSystemInfo();
            Motherboard = $"{sysInfo.MotherboardManufacturer} {sysInfo.MotherboardModel}";
            Processor = $"{sysInfo.ProcessorName} ({sysInfo.CoreCount}C/{sysInfo.ThreadCount}T)";
            Gpu = $"{sysInfo.GpuName} ({sysInfo.GpuMemory}GB)";
            OsName = $"{sysInfo.OsName} {sysInfo.OsVersion}";

            RefreshSystemMetrics();
        }

        protected void OnPropertyChanged([CallerMemberName] string name = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
        }
    }
}
