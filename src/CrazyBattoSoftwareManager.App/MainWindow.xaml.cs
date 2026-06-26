using System;
using System.Windows;
using System.Windows.Threading;
using CrazyBattoSoftwareManager.App.ViewModels;

namespace CrazyBattoSoftwareManager.App
{
    public partial class MainWindow : Window
    {
        private MainViewModel _viewModel;
        private DispatcherTimer _refreshTimer;

        public MainWindow()
        {
            InitializeComponent();
            
            // Set DataContext for binding
            _viewModel = new MainViewModel();
            this.DataContext = _viewModel;

            // Initialize refresh timer (updates every 1 second)
            _refreshTimer = new DispatcherTimer();
            _refreshTimer.Interval = TimeSpan.FromSeconds(1);
            _refreshTimer.Tick += (s, e) => _viewModel.RefreshSystemMetrics();
            _refreshTimer.Start();

            this.Closed += (s, e) => _refreshTimer.Stop();
        }
    }
}
