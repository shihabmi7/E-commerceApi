# Angular — Basic Interview Questions

## 1. What is Angular, and how is it different from AngularJS?
Angular is a TypeScript-based, component-driven front-end framework (versions 2+). AngularJS (1.x) is its JavaScript-based predecessor, built around `$scope` and controllers rather than components. They're effectively separate frameworks sharing a name — there's no direct upgrade path, only migration.

## 2. What is a component?
The basic building block of an Angular UI: a TypeScript class with a template (HTML) and styles, decorated with `@Component`.
```typescript
@Component({
  selector: 'app-employee-list',
  templateUrl: './employee-list.component.html',
  styleUrls: ['./employee-list.component.css']
})
export class EmployeeListComponent {
  employees: Employee[] = [];
}
```
`selector` is what you use as a custom HTML tag (`<app-employee-list></app-employee-list>`) to embed this component elsewhere.

## 3. What's the difference between a Module (NgModule) and a standalone component?
Historically, every component had to be declared inside an `NgModule` (`AppModule`, `EmployeeModule`, etc.), which grouped components/directives/pipes and imported other modules.
```typescript
@NgModule({
  declarations: [EmployeeListComponent],
  imports: [CommonModule, HttpClientModule],
})
export class EmployeeModule {}
```
Since Angular 14+, **standalone components** skip `NgModule` entirely — a component declares its own imports directly:
```typescript
@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './employee-list.component.html'
})
export class EmployeeListComponent {}
```
Modern Angular (17+) defaults new projects to standalone — worth mentioning both since plenty of existing codebases still use `NgModule`.

## 4. What are the types of data binding?
- **Interpolation**: `{{ employee.name }}` — component → template, text only.
- **Property binding**: `[value]="employee.name"` — component → template, any DOM property.
- **Event binding**: `(click)="deleteEmployee(employee.id)"` — template → component.
- **Two-way binding**: `[(ngModel)]="employee.name"` — both directions at once (needs `FormsModule`).

Two-way binding is really property binding + event binding combined under the hood — `[(ngModel)]` is shorthand for `[ngModel]="x" (ngModelChange)="x=$event"`.

## 5. What is a directive? Structural vs. attribute directives?
A directive adds behavior to an existing DOM element.
- **Structural** (change the DOM layout, prefixed `*`): `*ngIf="condition"`, `*ngFor="let e of employees"`.
- **Attribute** (change appearance/behavior of an existing element): `[ngClass]="{active: isActive}"`, `[ngStyle]`, or a custom `[appHighlight]`.

```html
<tr *ngFor="let employee of employees" [ngClass]="{'high-earner': employee.salary > 100000}">
  {{ employee.name }}
</tr>
```

## 6. What is a service, and how does dependency injection work?
A service is a plain TypeScript class holding logic/data meant to be shared across components (API calls, state) — decorated with `@Injectable`.
```typescript
@Injectable({ providedIn: 'root' })  // registers it as a singleton, app-wide
export class EmployeeService {
  constructor(private http: HttpClient) {}
  getAll(): Observable<Employee[]> {
    return this.http.get<Employee[]>('/api/employees');
  }
}
```
`providedIn: 'root'` registers the service with Angular's root injector — a component just declares it as a constructor parameter and Angular supplies the same singleton instance, the same DI pattern as Spring's constructor injection.
```typescript
constructor(private employeeService: EmployeeService) {}
```

## 7. What are the main component lifecycle hooks?
- `ngOnChanges` — fires when an `@Input()` property changes.
- `ngOnInit` — fires once, after the component's inputs are set — the usual place to fetch initial data.
- `ngAfterViewInit` — fires once the component's view (and child views) are fully initialized — needed before using `@ViewChild`.
- `ngOnDestroy` — fires right before the component is removed — the place to unsubscribe from Observables/clean up timers, to avoid memory leaks.

```typescript
export class EmployeeListComponent implements OnInit, OnDestroy {
  private sub!: Subscription;
  ngOnInit() {
    this.sub = this.employeeService.getAll().subscribe(data => this.employees = data);
  }
  ngOnDestroy() {
    this.sub.unsubscribe();
  }
}
```

## 8. What is RxJS, and why does Angular use Observables instead of Promises?
RxJS is a library for composing asynchronous/event-based logic using **Observables** — streams of values over time. Angular's `HttpClient` returns Observables rather than Promises.

Key differences from a Promise:
- A Promise resolves once; an Observable can emit multiple values over time (e.g. a WebSocket stream, or `valueChanges` on a form).
- Observables are **lazy** — nothing happens until you call `.subscribe()`. A Promise's executor runs immediately on creation.
- Observables are **cancellable** (`subscription.unsubscribe()`); a Promise, once started, can't be cancelled.
- RxJS provides composable operators (`map`, `filter`, `switchMap`, `debounceTime`) for transforming/combining streams — e.g. debouncing a search input before calling an API.

## 9. What is the `async` pipe, and why use it over manual subscription?
```html
<tr *ngFor="let employee of employees$ | async">{{ employee.name }}</tr>
```
The `async` pipe subscribes to an Observable (or Promise) directly in the template and **automatically unsubscribes** when the component is destroyed — removing the need for a manual `ngOnDestroy` unsubscribe (Q7) for that specific stream. It also triggers change detection automatically when a new value arrives.

## 10. What is Angular routing, and how do route guards work?
`RouterModule` maps URL paths to components:
```typescript
const routes: Routes = [
  { path: 'employees', component: EmployeeListComponent },
  { path: 'employees/:id', component: EmployeeDetailComponent, canActivate: [AuthGuard] },
];
```
`routerLink="/employees"` navigates without a full page reload (client-side routing). A **guard** (`canActivate`, `canDeactivate`) is a class implementing a check that runs before a route resolves — e.g. `AuthGuard` returning `false`/redirecting if the user isn't logged in, the routing-layer equivalent of `@PreAuthorize` in Spring Security.

## 11. What is a pipe? Built-in vs. custom.
A pipe transforms a displayed value in the template without changing the underlying data.
```html
{{ employee.joinDate | date:'mediumDate' }}
{{ employee.salary | currency:'USD' }}
```
Built-in pipes: `date`, `currency`, `uppercase`, `json`, `async` (Q9). A custom pipe implements `PipeTransform`:
```typescript
@Pipe({ name: 'truncate' })
export class TruncatePipe implements PipeTransform {
  transform(value: string, limit = 20): string {
    return value.length > limit ? value.slice(0, limit) + '...' : value;
  }
}
```

## 12. What is change detection, and what does `OnPush` do?
Angular's mechanism for detecting when component data changes and updating the DOM to match. By default (`ChangeDetectionStrategy.Default`), Angular checks every component on every browser event (click, timer, HTTP response) via Zone.js patching async APIs — safe, but can be wasteful on large component trees.

`ChangeDetectionStrategy.OnPush` tells a component to skip that default check and only re-render when: an `@Input()` reference changes, an event originates from within the component itself, or an Observable bound via `async` emits.
```typescript
@Component({ changeDetection: ChangeDetectionStrategy.OnPush, ... })
```
The common gotcha: with `OnPush`, mutating an `@Input()` object in place (`employee.salary = 5000`) won't trigger re-render, because the object *reference* didn't change — you need to pass a new object/array reference instead (immutable update pattern).

## 13. What's the difference between `@ViewChild` and `@ContentChild`?
`@ViewChild` gets a reference to an element/component defined in *this component's own template*. `@ContentChild` gets a reference to something *projected into* this component via `<ng-content>` (i.e., passed in by whatever's using this component). Both are only reliably available after `ngAfterViewInit`/`ngAfterContentInit` respectively (Q7).

## 14. What is lazy loading, and why use it?
Splitting the app into separate JavaScript bundles per feature, loaded on demand instead of all upfront.
```typescript
{ path: 'employees', loadChildren: () => import('./employee/employee.module').then(m => m.EmployeeModule) }
```
The `employee` feature's code only downloads when the user actually navigates to `/employees`, shrinking the initial bundle size and speeding up first load — the front-end equivalent of not eagerly initializing every Spring bean you might never use.

## 15. Template-driven vs. reactive forms — what's the difference?
- **Template-driven**: form structure/validation defined mostly in the HTML template, using `[(ngModel)]` and directives (`required`, `minlength`). Simpler for basic forms, needs `FormsModule`.
- **Reactive**: form structure defined in the component class as a `FormGroup`/`FormControl` tree, template just binds to it. More explicit, easier to unit test, better for complex/dynamic forms. Needs `ReactiveFormsModule`.
```typescript
employeeForm = new FormGroup({
  firstName: new FormControl('', [Validators.required]),
  email: new FormControl('', [Validators.required, Validators.email]),
});
```
```html
<form [formGroup]="employeeForm">
  <input formControlName="firstName" />
</form>
```
Reactive forms are generally preferred in larger apps for the same reason constructor injection is preferred over field injection in Spring (Q4 in the Spring Boot doc) — the dependencies/structure are explicit in code, not implicit in markup, which makes them easier to test.
