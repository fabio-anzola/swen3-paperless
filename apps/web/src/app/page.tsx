import { redirect } from "next/navigation";
import { HOME_ROUTE } from "@/config";

export default function Home() {
  redirect(HOME_ROUTE);
}
