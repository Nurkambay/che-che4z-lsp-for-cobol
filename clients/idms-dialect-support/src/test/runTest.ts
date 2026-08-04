/*
 * Copyright (c) 2023 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom - initial API and implementation
 */

import * as path from "path";

import { runTests } from '@vscode/test-electron';
import * as os from 'os';

async function main() {
	try {
		// The folder containing the Extension Manifest package.json
		// Passed to `--extensionDevelopmentPath`
		const extensionDevelopmentPath = path.resolve(__dirname, '../../');
		// The path to test runner
		// Passed to --extensionTestsPath
		const extensionTestsPath = path.resolve(__dirname, './suite/index');

		const launchArgs = ['--user-data-dir', `${os.tmpdir()}`];

		// Download VS Code, unzip it and run the integration test
		// VS Code >=1.131 removed the `Contents/MacOS/Electron` compatibility
		// symlink on macOS (only `Code` remains), which breaks how VS Code is
		// launched for extension tests: https://github.com/microsoft/vscode-test/issues/348
		// Pin to the last version that still ships the symlink until that's fixed upstream.
		await runTests({ version: '1.130.0', extensionDevelopmentPath, extensionTestsPath, launchArgs });
	} catch (err) {
		console.error('Failed to run tests');
		process.exit(1);
	}
}
main();
