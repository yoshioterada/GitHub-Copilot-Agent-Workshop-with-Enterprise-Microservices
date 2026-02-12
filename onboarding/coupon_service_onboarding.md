# coupon-service オンボーディングガイド

対象者: 新入社員（バックエンド初学者〜中級）

目的:
- `coupon-service` を触り、簡単な CRUD エンドポイントの追加、テスト作成、ローカル起動と PR 作成の流れを学ぶ。

前提条件:
- Java (11+)、Maven がインストールされていること
- Docker と Docker Compose が使えること（ローカルDB が必要な場合）
- リポジトリをクローン済みであること

ファイル位置（参照）:
- サービスルート: `coupon-service/`
- ソース: `coupon-service/src/main/java/...`（パッケージ構成は既存に合わせる）

タスク概要（小さなステップに分割）

ステップ A — ローカルでビルドして実行する（Day0）
1. リポジトリのルートで `coupon-service` ディレクトリへ移動
2. ビルド: `mvn -q -DskipTests package`
3. 必要なら Docker Compose を使って依存サービスを起動（例: DB）
   - `docker-compose -f coupon-service/docker-compose.yml up -d`
4. アプリを起動（IDE から、または `mvn spring-boot:run`）
5. ブラウザや curl で既存エンドポイントが応答するか確認

ステップ B — ヘルスチェックエンドポイントの確認／追加（1日）
1. `src/main/resources/application.properties` に Actuator 設定があるか確認。
2. Actuator が無ければ `pom.xml` に `spring-boot-starter-actuator` を追加。
3. 必要なら `HealthIndicator` を実装してカスタム情報を返す。
4. `src/test/java` に MockMvc を使った簡単なテストを追加。

ステップ C — GET /coupons/{id} エンドポイントを追加（1-2日）
目的: Controller → Service → Repository の基本フローを実装

実装手順:
1. 既存のパッケージ構成を確認。例: `com.skishop.coupon` に沿う形で作成。
2. モデル（Entity / DTO）を確認または作成。
   - まずは DTO だけで実装（Repository をモック）して API の形を作るのがおすすめ。
3. Controller を追加: `CouponController` に `@GetMapping("/coupons/{id}")` を実装。
4. Service 層を追加: `CouponService#getCouponById(String id)` を実装。
5. Repository 層: 既に JPA が使われているなら `CouponRepository extends JpaRepository<Coupon, String>` を作成。簡易版ならインメモリの Map を使った実装で良い。

サンプル Controller（雛形）:

// ここは参照用の擬似コードです。実際は既存のパッケージや型に合わせてください。

public class CouponController {
    private final CouponService couponService;
    @GetMapping("/coupons/{id}")
    public ResponseEntity<CouponDto> getCoupon(@PathVariable String id) {
        return ResponseEntity.of(couponService.findById(id));
    }
}

ステップ D — テストの追加（必須）
1. Controller のユニットテスト（MockMvc + Mockito）を追加。
2. Repository を使うなら H2 を使った軽量な統合テストを追加。
3. `mvn test` が通ることを確認。

ステップ E — ドキュメントと PR（半日）
1. `README.md` の `coupon-service` セクションに「起動手順」「エンドポイント一覧」「テスト実行方法」を追記。
2. PR を作成。PR 本文に以下を記載すること:
   - 変更の要約（what）
   - なぜ必要か（why）
   - 動作確認手順（how to test）
   - テスト結果（ローカル mvn test の出力概要）

サンプルテストケース（例）:
- Controller: 存在する ID の場合 200 と JSON、存在しない ID の場合 404 を返す
- Repository: H2 で保存→取得が成功する

AI（この同AI）に頼めるサポート例
- Controller/DTO/Service の雛形コード生成
- MockMvc 用テストテンプレート生成
- README の文言校正や PR 本文のテンプレ雛形

チェックリスト（実装完了時）
- [ ] `mvn test` がローカルで通る
- [ ] GET /coupons/{id} のエンドポイントを実際に curl で試した
- [ ] README に手順を追記して PR を作成

次のステップ（私が代行する場合）
- 要望があれば、実際に `coupon-service` のソースを読み、Controller とテストの雛形を生成してコミットします。ご希望ならそのまま PR 作成用ブランチを用意します。

----
ファイル: `onboarding/coupon_service_onboarding.md`
