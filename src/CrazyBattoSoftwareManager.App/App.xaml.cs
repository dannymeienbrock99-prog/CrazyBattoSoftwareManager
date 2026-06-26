using System;
using System.Windows;
using System.Windows.Input;

namespace CrazyBattoSoftwareManager.App
{
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // Optional: Add global exception handling
            this.DispatcherUnhandledException += (s, args) =>
            {
                MessageBox.Show(
                    $"Ein Fehler ist aufgetreten:\n{args.Exception.Message}",
                    "CrazyBatto - Fehler",
                    MessageBoxButton.OK,
                    MessageBoxImage.Error);
                args.Handled = true;
            };
        }
    }
}
