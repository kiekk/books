import { NextFunction, Request, Response, Router } from "express";
import { BaseRoute } from "./baseRoutes";
import { Index } from "../controllers";

export class AppRoutes extends BaseRoute {

    constructor() {
        super();
    }

    public static create(router: Router) {
        // @ts-ignore
        router.get("/", (req: Request, res: Response, next: NextFunction) => {
            new Index().renderView(req, res, next);
        });

        // @ts-ignore
        router.post("/add", (req: Request, res: Response, next: NextFunction) => {
            new Index().add(req, res, next);
        });

    }

}