export const exampleOasYaml = `openapi: 3.0.1
info:
  title: ''
  description: |-
    *Insert description of service*

    *Include version history at top level, e.g.*
      | Version | Date       | Author         | Description       |
      |---------|------------|----------------|-------------------|
      | 1.0.0   | 01-02-2025 | Publisher Name | Initial version   |
  license:
    name: Apache 2.0
    url: http://www.apache.org/licenses/LICENSE-2.0
  version: 1.0.0
paths:
  /my-company/employees/{employeeId}:
    get:
      tags:
      - Get employee
      summary: Get employee details
      description: |
        *Insert description of endpoint* \`/my-company/employees/a100\`
      security:
      - oAuth2:
        - read:my-company-employees
      parameters:
        - $ref: '#/components/parameters/correlationId'
        - $ref: '#/components/parameters/employeeId'
      responses:
        "200":
          description: Success
          headers:
            correlationId:
              $ref: '#/components/headers/correlationId'
          content:
            application/json;charset=UTF-8:
              schema:
                $ref: '#/components/schemas/successResponse'
              examples:
                example 1:
                  $ref: '#/components/examples/successResponseExample'
        "400":
          description: Bad request
          headers:
            correlationId:
              $ref: '#/components/headers/correlationId'
          content:
            application/json;charset=UTF-8:
              schema:
                required:
                - response
                type: object
                properties:
                  response:
                    $ref: '#/components/schemas/failureResponse'
                additionalProperties: false
              examples:
                example 1:
                  $ref: '#/components/examples/failureResponse400Example'
        '404':
          description: Not found
          headers:
            correlationId:
              $ref: '#/components/headers/correlationId'
          content:
            application/json;charset=UTF-8:
              schema:
                required:
                - response
                type: object
                properties:
                  response:
                    $ref: '#/components/schemas/failureResponse'
                additionalProperties: false
              examples:
                example 1:
                  $ref: '#/components/examples/failureResponse404Example'
        '500':
          description: Server error
          headers:
            correlationId:
              $ref: '#/components/headers/correlationId'
          content:
            application/json;charset=UTF-8:
              schema:
                required:
                - response
                type: object
                properties:
                  response:
                    $ref: '#/components/schemas/failureResponse'
                additionalProperties: false
              examples:
                example 1:
                  $ref: '#/components/examples/failureResponse500Example'
  /my-company/employees:
    put:
      tags:
      - Update employee
      summary: Update employees details
      description: |
        *Insert description of endpoint* \`/my-company/employees/\`
      security:
      - oAuth2:
        - write:my-company-employees
      parameters:
        - $ref: '#/components/parameters/correlationId'
      requestBody:
        required: true
        description: Request
        content:
          application/json;charset=UTF-8:
            schema:
              $ref: '#/components/schemas/request'
            examples:
              example 1:
                $ref: '#/components/examples/requestExample'
      responses:
        "204":
          description: Success
        "400":
          description: Bad request
          headers:
            correlationId:
              $ref: '#/components/headers/correlationId'
          content:
            application/json;charset=UTF-8:
              schema:
                required:
                - response
                type: object
                properties:
                  response:
                    $ref: '#/components/schemas/failureResponse'
                additionalProperties: false
              examples:
                example 1:
                  $ref: '#/components/examples/failureResponse400Example'
        '404':
          description: Not found
          headers:
            correlationId:
              $ref: '#/components/headers/correlationId'
          content:
            application/json;charset=UTF-8:
              schema:
                required:
                - response
                type: object
                properties:
                  response:
                    $ref: '#/components/schemas/failureResponse'
                additionalProperties: false
              examples:
                example 1:
                  $ref: '#/components/examples/failureResponse404Example'
        '500':
          description: Server error
          headers:
            correlationId:
              $ref: '#/components/headers/correlationId'
          content:
            application/json;charset=UTF-8:
              schema:
                required:
                - response
                type: object
                properties:
                  response:
                    $ref: '#/components/schemas/failureResponse'
                additionalProperties: false
              examples:
                example 1:
                  $ref: '#/components/examples/failureResponse500Example'
components:
  headers:
    correlationId:
      description: |
        used for end to end traceability purposes and should be considered mandatory. In the
        event, however, it cannot be provided, the HIP framework will generate it, to allow
        for downstream auditing
      schema:
        type: string
        pattern: ^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$
  parameters:
    correlationId:
      in: header
      name: correlationId
      description: |
        Used for end to end traceability purposes and should be considered mandatory. In the
        event, however, it cannot be provided, the HIP framework will generate it, to allow
        for downstream auditing
      required: false
      schema:
        type: string
        pattern: ^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$
    employeeId:
      in: path
      name: employeeId
      description: Identifies the specific resource and does not contain spaces
      required: true
      schema:
        type: string
        pattern: ^(a100|b100)$
  schemas:
    failureResponse:
      required:
      - failures
      type: object
      properties:
       failures:
         minItems: 1
         uniqueItems: true
         type: array
         items:
           required:
           - type
           - reason
           type: object
           properties:
             type:
               type: string
             reason:
               type: string
           additionalProperties: false
    request:
      required:
      - employeeId
      - nameDetails
      type: object
      properties:
        employeeId:
          type: string
          pattern: ^(a100|b100)$
        nameDetails:
          required:
          - firstName
          - lastName
          type: object
          properties:
            firstName:
              type: string
              description: First name.
              pattern: ^[a-zA-Z &\`\\-\\'^]{1,35}$
            lastName:
              type: string
              description: Last name.
              pattern: ^[a-zA-Z &\`\\-\\'^]{1,35}$
    successResponse:
      required:
      - firstName
      - lastName
      type: object
      properties:
        firstName:
          type: string
          description: First name.
          pattern: ^[a-zA-Z &\`\\-\\'^]{1,35}$
        lastName:
          type: string
          description: Last name.
          pattern: ^[a-zA-Z &\`\\-\\'^]{1,35}$
  examples:
    failureResponse400Example:
      value:
        response:
          failures:
            - type: INVALID_EMPLOYEEID
              reason: Submission has not passed validation. Invalid employeeId.
    failureResponse404Example:
      value:
        response:
          failures:
            - type: NOT_FOUND
              reason: The employeeId could not be found.
    failureResponse500Example:
      value:
        response:
          failures:
           - type: SERVER_ERROR
             reason: The backend service is currently experiencing issues.
    requestExample:
      value:
        employeeId: a100
        nameDetails:
          firstName: Mary
          lastName: Smith
    successResponseExample:
      value:
        firstName: Mary
        lastName: Jones
  securitySchemes:
    oAuth2:
      type: oauth2
      description: OAuth2 Client Credentials Flow
      flows:
        clientCredentials:
          tokenUrl: /tokenUrl/not-required
          scopes:
            read:my-company-employees: Company Employees
            write:my-company-employees: Company Employees
`;