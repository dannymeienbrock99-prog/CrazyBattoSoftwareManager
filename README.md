# CrazyBatto SOFTWARE MANAGER

Ein professionelles **System-Monitor & Optimierungs-Tool** für Gaming-PCs.

## 🎯 Features

- **Live System Monitoring**: CPU, RAM, GPU, Temperaturen in Echtzeit
- **System Health Check**: Automatische Diagnose
- **Windows Tweaks**: Performance-Optimierungen
- **Plugin System**: Erweiterbar mit Plugins
- **Wasserkühlung Monitoring**: D5 NEXT & Custom Loops
- **ASUS/ROG Integration**: Diagnose & Support
- **Overlay**: In-Game Performance Display
- **Reports**: Detaillierte System-Reports

## 📁 Projektstruktur

```
src/
├── CrazyBattoSoftwareManager.App/
│   ├── MainWindow.xaml              # Main UI
│   ├── MainWindow.xaml.cs           # Code-Behind
│   ├── App.xaml                     # App Resources (Colors, Styles, Fonts)
│   ├── App.xaml.cs                  # App Logic
│   ├── Services/
│   │   └── SystemInfoService.cs     # System Monitoring
│   ├── ViewModels/
│   │   └── MainViewModel.cs         # Data Binding & Logic
│   └── Assets/
│       └── dragon.png               # Dragon Image
```

## 🚀 Installation & Setup

### Anforderungen
- **.NET 6.0+** oder **WPF (.NET Framework 4.8+)**
- **Windows 10/11**
- **Administrator-Rechte** (für System-Monitoring)

### Build & Run

```bash
# Clone Repository
git clone https://github.com/dannymeienbrock99-prog/CrazyBattoSoftwareManager.git
cd CrazyBattoSoftwareManager

# Build
dotnet build

# Run
dotnet run --project src/CrazyBattoSoftwareManager.App
```

## 🔧 Architektur

### MVVM Pattern
- **View** (MainWindow.xaml) → UI
- **ViewModel** (MainViewModel.cs) → Business Logic & Data Binding
- **Model** (SystemInfoService.cs) → System Data

### Data Binding
```csharp
// MainWindow.xaml.cs
_viewModel = new MainViewModel();
this.DataContext = _viewModel;

// Auto-Updates alle 1 Sekunde
_refreshTimer.Interval = TimeSpan.FromSeconds(1);
_refreshTimer.Tick += (s, e) => _viewModel.RefreshSystemMetrics();
```

### System Monitoring
```csharp
// SystemInfoService.cs
GetCpuUsage()        // CPU Auslastung
GetRamUsage()        // RAM Auslastung
GetCpuTemperature()  // CPU Temperatur
GetGpuTemperature()  // GPU Temperatur
GetDiskSpace()       // Festplatte
GetSystemInfo()      // Motherboard, CPU, GPU, OS
```

## 🎨 Design

- **Farben**: #111827 (Primary), #1E9BFF (Accent), #EEF3F8 (Background)
- **Grid Layout**: 295px Sidebar + Flex Content
- **Responsive**: MinWidth 1280px, MinHeight 760px
- **Theme**: Modern Gaming UI mit Dragon-Motiv

## 📊 Live Metrics (Real-time)

Dashboard zeigt:
- **CPU**: Auslastung + Temperatur
- **RAM**: Verfügbar / Gesamt
- **GPU**: Model + Memory + Temp
- **Disk**: Verwendet / Gesamt
- **System**: Motherboard, CPU Cores, OS

## 🔌 Services

### SystemInfoService
Erfasst System-Informationen via **Windows WMI**:
- Performance Counter (CPU, RAM)
- Thermal Zone Data (Temperaturen)
- Drive Info (Speicher)
- Win32 Classes (Hardware-Info)

## 📝 Lizenz

MIT License - Frei verwendbar für Gaming PCs

## 👨‍💻 Autor

**dannymeienbrock99-prog**

---

**CrazyBatto - Alles in einer Software!** 🐉⚡
