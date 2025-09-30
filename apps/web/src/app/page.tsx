import { HOME_ROUTE } from "@/config";
import { redirect } from "next/navigation";

export default function Home() {
  redirect(HOME_ROUTE);
}
