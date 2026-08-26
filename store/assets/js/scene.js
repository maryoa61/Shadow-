// ===== صحنه‌ی سه‌بعدی پس‌زمینه — SHADOW_STORE =====
import * as THREE from "three";

const canvas = document.getElementById("bg3d");
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);

const scene = new THREE.Scene();
scene.fog = new THREE.FogExp2(0x06070d, 0.045);

const camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 100);
camera.position.set(0, 0, 14);

// نورها
const ambient = new THREE.AmbientLight(0x8899ff, 0.55);
scene.add(ambient);
const keyLight = new THREE.PointLight(0x7c3aed, 120, 60);
keyLight.position.set(-8, 6, 6);
scene.add(keyLight);
const rimLight = new THREE.PointLight(0x22d3ee, 120, 60);
rimLight.position.set(9, -4, 4);
scene.add(rimLight);

// اشکال شناور
const shapes = [];
const geoFactories = [
  () => new THREE.IcosahedronGeometry(1.4, 0),
  () => new THREE.TorusKnotGeometry(0.9, 0.28, 140, 18),
  () => new THREE.OctahedronGeometry(1.3, 0),
  () => new THREE.TorusGeometry(1.1, 0.4, 18, 60),
  () => new THREE.DodecahedronGeometry(1.2, 0),
  () => new THREE.ConeGeometry(1, 1.8, 6),
];

function makeMaterial(color, wire = false) {
  return new THREE.MeshStandardMaterial({
    color,
    metalness: 0.75,
    roughness: 0.22,
    wireframe: wire,
    transparent: true,
    opacity: 0.92,
  });
}

const palette = [0x7c3aed, 0x22d3ee, 0xf472b6, 0x6366f1, 0x34d399, 0xf59e0b];

for (let i = 0; i < 16; i++) {
  const geo = geoFactories[i % geoFactories.length]();
  const mat = makeMaterial(palette[i % palette.length], i % 4 === 3);
  const mesh = new THREE.Mesh(geo, mat);

  const radius = 6 + Math.random() * 9;
  const angle = Math.random() * Math.PI * 2;
  mesh.position.set(
    Math.cos(angle) * radius,
    (Math.random() - 0.5) * 16,
    (Math.random() - 0.5) * 14 - 4
  );
  mesh.rotation.set(Math.random() * Math.PI, Math.random() * Math.PI, 0);
  mesh.userData = {
    baseY: mesh.position.y,
    speed: 0.25 + Math.random() * 0.6,
    rotX: (Math.random() - 0.5) * 0.006,
    rotY: (Math.random() - 0.5) * 0.008,
    floatAmp: 0.6 + Math.random() * 1.2,
    phase: Math.random() * Math.PI * 2,
  };
  shapes.push(mesh);
  scene.add(mesh);
}

// ستاره‌ها / ذرات
const particleCount = 900;
const positions = new Float32Array(particleCount * 3);
for (let i = 0; i < particleCount; i++) {
  positions[i * 3] = (Math.random() - 0.5) * 70;
  positions[i * 3 + 1] = (Math.random() - 0.5) * 50;
  positions[i * 3 + 2] = (Math.random() - 0.5) * 40 - 6;
}
const pGeo = new THREE.BufferGeometry();
pGeo.setAttribute("position", new THREE.BufferAttribute(positions, 3));
const pMat = new THREE.PointsMaterial({
  color: 0x9db4ff,
  size: 0.055,
  transparent: true,
  opacity: 0.7,
  sizeAttenuation: true,
});
const particles = new THREE.Points(pGeo, pMat);
scene.add(particles);

// شبکه‌ی وایرفریم کف
const grid = new THREE.GridHelper(60, 42, 0x2a2f4a, 0x141830);
grid.position.y = -10.5;
grid.rotation.x = 0;
grid.material.transparent = true;
grid.material.opacity = 0.5;
scene.add(grid);

// تعامل با موس و اسکرول
let mouseX = 0, mouseY = 0, scrollT = 0;
window.addEventListener("pointermove", (e) => {
  mouseX = (e.clientX / window.innerWidth - 0.5) * 2;
  mouseY = (e.clientY / window.innerHeight - 0.5) * 2;
});
window.addEventListener("scroll", () => {
  scrollT = window.scrollY / Math.max(1, document.body.scrollHeight - window.innerHeight);
});

window.addEventListener("resize", () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
});

// حلقه‌ی رندر
const clock = new THREE.Clock();
function animate() {
  requestAnimationFrame(animate);
  const t = clock.getElapsedTime();

  for (const m of shapes) {
    const u = m.userData;
    m.rotation.x += u.rotX;
    m.rotation.y += u.rotY;
    m.position.y = u.baseY + Math.sin(t * u.speed + u.phase) * u.floatAmp;
  }

  particles.rotation.y = t * 0.012;
  grid.position.z = (t * 0.7) % 1.43;

  keyLight.position.x = -8 + Math.sin(t * 0.4) * 4;
  rimLight.position.y = -4 + Math.cos(t * 0.35) * 3;

  // پارالاکس نرم دوربین
  camera.position.x += (mouseX * 1.6 - camera.position.x) * 0.04;
  camera.position.y += (-mouseY * 1.1 - scrollT * 6 - camera.position.y) * 0.05;
  camera.lookAt(0, -scrollT * 5, 0);

  renderer.render(scene, camera);
}
animate();
