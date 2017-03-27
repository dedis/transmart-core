import { ModuleWithProviders }  from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { DataSelectionComponent } from './components/data-selection/data-selection.component';
import { DataViewComponent } from './components/data-view/data-view.component';
import { AnalysisComponent } from './components/analysis/analysis.component';

// Route Configuration
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    component: DashboardComponent },
  {
    path: 'data-selection',
    component: DataSelectionComponent },
  {
    path: 'data-view',
    component: DataViewComponent },
  {
    path: 'analysis',
    component: AnalysisComponent }
];

export const routing: ModuleWithProviders = RouterModule.forRoot(routes);