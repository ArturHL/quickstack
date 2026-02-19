type NavigateFn = (path: string) => void

let _navigate: NavigateFn | null = null

export function registerNavigate(fn: NavigateFn): void {
  _navigate = fn
}

export function imperativeNavigate(path: string): void {
  if (_navigate) {
    _navigate(path)
  } else {
    window.location.href = path
  }
}
