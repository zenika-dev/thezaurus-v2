import { SideMenuClient } from "./SideMenuClient";

const navLinks = [
  { label: "Événements", path: "/" },
  { label: "Talks",       path: "/talks" },
  { label: "Conférences", path: "/conferences" },
  { label: "Blog Posts",  path: "/blog-posts" },
];

export function SideMenu() {
  return <SideMenuClient navLinks={navLinks} />;
}

export default SideMenu;
