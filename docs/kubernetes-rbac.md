# Kubernetes RBAC for Homelab Homepage

`nbot`'s root homepage (`/`) queries the Kubernetes API across all namespaces for Traefik `IngressRoute` resources (`traefik.io/v1alpha1`).

When deploying `nbot` into the `nlab` cluster (namespace `nbot`), grant the pod's `ServiceAccount` read-only cluster-wide access to `ingressroutes.traefik.io`.

---

## Option 1: `bjw-s/app-template` Values (`k8s/10_apps/10_nbot/values/nbot.values.yaml`)

Add `serviceAccount`, `rbac`, and `controllers.main.serviceAccount` to `nbot.values.yaml`:

```yaml
controllers:
  main:
    serviceAccount:
      identifier: nbot

serviceAccount:
  nbot: {}

rbac:
  roles:
    nbot-ingressroute-reader:
      type: ClusterRole
      rules:
        - apiGroups:
            - traefik.io
          resources:
            - ingressroutes
          verbs:
            - get
            - list
            - watch
  bindings:
    nbot-ingressroute-reader:
      type: ClusterRoleBinding
      roleRef:
        identifier: nbot-ingressroute-reader
      subjects:
        - identifier: nbot
```

> **Note:** If you also want `home.local.naguiar.dev` / `home.naguiar.dev` to route to `nbot`, add those hosts to `rawResources.ingress-route.manifest.spec.routes[0].match` in `nbot.values.yaml`.

---

## Option 2: Standalone Kubernetes Manifests

If you prefer raw manifests instead of `bjw-s/app-template` RBAC keys:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: nbot
  namespace: nbot
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: nbot-ingressroute-reader
rules:
  - apiGroups:
      - traefik.io
    resources:
      - ingressroutes
    verbs:
      - get
      - list
      - watch
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: nbot-ingressroute-reader
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: nbot-ingressroute-reader
subjects:
  - kind: ServiceAccount
    name: nbot
    namespace: nbot
```

---

## Optional Environment Variables

| Variable | Default | Description |
| :--- | :--- | :--- |
| `HOMEPAGE_K8S_ENABLED` | `true` | Enables/disables Kubernetes `IngressRoute` discovery. |
| `HOMEPAGE_CACHE_TTL_SECONDS` | `30` | In-memory cache TTL (seconds) for cluster `IngressRoute` queries. |
